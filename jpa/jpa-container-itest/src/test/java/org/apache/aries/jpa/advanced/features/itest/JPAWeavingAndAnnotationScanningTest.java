/*  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.jpa.advanced.features.itest;

import static org.junit.Assert.assertEquals;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.aries.jpa.container.advanced.itest.bundle.entities.Car;
import org.apache.aries.jpa.itest.AbstractJPAItest;
import org.junit.Test;

public abstract class JPAWeavingAndAnnotationScanningTest extends AbstractJPAItest {

	@Test
	public void testAnnotatedClassFound() throws Exception {
		Thread.sleep(20000);
	  EntityManagerFactory emf = getEMF(TEST_UNIT);
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();

		Car c = new Car();
		c.setColour("Blue");
		c.setNumberPlate("AB11CDE");
		c.setNumberOfSeats(7);
		c.setEngineSize(1900);
		em.persist(c);

		em.getTransaction().commit();

		assertEquals(7, em.find(Car.class, "AB11CDE").getNumberOfSeats());
	}

}
