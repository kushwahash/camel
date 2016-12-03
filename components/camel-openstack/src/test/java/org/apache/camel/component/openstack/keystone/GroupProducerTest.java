/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.openstack.keystone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.camel.component.openstack.keystone.producer.GroupProducer;
import org.apache.camel.component.openstack.neutron.NeutronConstants;

import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.openstack4j.api.Builders;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.identity.v3.Group;
import org.openstack4j.model.network.Network;

import java.util.ArrayList;
import java.util.List;

public class GroupProducerTest extends KeystoneProducerTestSupport {

	private Group dummyGroup;

	@Mock
	private Group testOSgroup;

	@Before
	public void setUp() {
		producer = new GroupProducer(endpoint, client);
		
		when(groupService.create(any(Group.class))).thenReturn(testOSgroup);
		when(groupService.get(anyString())).thenReturn(testOSgroup);

		List<Group> getAllList = new ArrayList<>();
		getAllList.add(testOSgroup);
		getAllList.add(testOSgroup);
		doReturn(getAllList).when(groupService).list();
		
		dummyGroup = createGroup();

		when(testOSgroup.getName()).thenReturn(dummyGroup.getName());
		when(testOSgroup.getDescription()).thenReturn(dummyGroup.getDescription());
	}
	
	@Test
	public void createTest() throws Exception {
		msg.setHeader(KeystoneConstants.OPERATION, KeystoneConstants.CREATE);
		msg.setHeader(KeystoneConstants.NAME, dummyGroup.getName());
		msg.setHeader(KeystoneConstants.DESCRIPTION, dummyGroup.getDescription());
		msg.setHeader(KeystoneConstants.DOMAIN_ID, dummyGroup.getDomainId());


		producer.process(exchange);

		ArgumentCaptor<Group> captor = ArgumentCaptor.forClass(Group.class);
		verify(groupService).create(captor.capture());

		assertEqualsGroup(dummyGroup, captor.getValue());
	}

	@Test
	public void getTest() throws Exception {
		final String id = "id";
		msg.setHeader(NeutronConstants.OPERATION, NeutronConstants.GET);
		msg.setHeader(NeutronConstants.ID, id);

		producer.process(exchange);

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(groupService).get(captor.capture());

		assertEquals(id, captor.getValue());
		assertEqualsGroup(testOSgroup, msg.getBody(Group.class));
	}

	@Test
	public void getAllTest() throws Exception {
		msg.setHeader(NeutronConstants.OPERATION, NeutronConstants.GET_ALL);

		producer.process(exchange);

		final List<Network> result = msg.getBody(List.class);
		assertTrue(result.size() == 2);
		assertEquals(testOSgroup, result.get(0));
	}

	@Test
	public void updateTest() throws Exception {
		final String id = "myID";
		msg.setHeader(KeystoneConstants.OPERATION, KeystoneConstants.UPDATE);
		final String newName = "newName";

		when(testOSgroup.getId()).thenReturn(id);
		when(testOSgroup.getName()).thenReturn(newName);
		when(testOSgroup.getDescription()).thenReturn("desc");

		when(groupService.update(any(Group.class))).thenReturn(testOSgroup);
		msg.setBody(testOSgroup);

		producer.process(exchange);

		ArgumentCaptor<Group> captor = ArgumentCaptor.forClass(Group.class);
		verify(groupService).update(captor.capture());

		assertEqualsGroup(testOSgroup, captor.getValue());
		assertNotNull(captor.getValue().getId());
		assertEquals(newName, msg.getBody(Group.class).getName());
	}
	
	@Test
	public void deleteTest() throws Exception {
		when(groupService.delete(anyString())).thenReturn(ActionResponse.actionSuccess());
		final String networkID = "myID";
		msg.setHeader(NeutronConstants.OPERATION, NeutronConstants.DELETE);
		msg.setHeader(NeutronConstants.ID, networkID);

		producer.process(exchange);

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(groupService).delete(captor.capture());
		assertEquals(networkID, captor.getValue());
		assertFalse(msg.isFault());

		//in case of failure
		final String failureMessage = "fail";
		when(groupService.delete(anyString())).thenReturn(ActionResponse.actionFailed(failureMessage, 404));
		producer.process(exchange);
		assertTrue(msg.isFault());
		assertTrue(msg.getBody(String.class).contains(failureMessage));
	}

	private void assertEqualsGroup(Group old, Group newGroup) {
		assertEquals(old.getName(), newGroup.getName());
		assertEquals(old.getDescription(), newGroup.getDescription());
		assertEquals(old.getDomainId(), newGroup.getDomainId());
	}

	private Group createGroup() {
		return Builders.group()
				.domainId("domain")
				.description("desc")
				.name("group Name").build();
	}

}