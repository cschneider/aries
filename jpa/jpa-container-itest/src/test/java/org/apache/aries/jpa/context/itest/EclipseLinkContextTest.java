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
package org.apache.aries.jpa.context.itest;

import org.apache.aries.jpa.container.itest.entities.Car;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;

public class EclipseLinkContextTest extends JPAContextTest {
  @Test
  public void testDeleteQuery() throws Exception {
    EntityManagerFactory emf = getEMF(BP_TEST_UNIT);
    EntityManager em = emf.createEntityManager();

    try {
      ut.begin();

      Car c = new Car();
      c.setColour("Blue");
      c.setNumberPlate("AB11CDE");
      c.setNumberOfSeats(7);
      c.setEngineSize(1900);
      em.persist(c);

      ut.commit();

    } finally {
      ut.rollback();
    }

    assertEquals(7, em.find(Car.class, "AB11CDE").getNumberOfSeats());

    em.close();

    em = emf.createEntityManager();

    CriteriaBuilder cb = em.getCriteriaBuilder();
    Method createCriteriaDelete = cb.getClass().getMethod("createCriteriaDelete", Class.class);
    final List<Object> l = new ArrayList<Object>();
    l.add(Car.class);
    Object criteriaDelete = createCriteriaDelete.invoke(cb, Car.class);
    Method from = CriteriaDelete.class.getMethod("from", Class.class);
    from.invoke(criteriaDelete, Car.class);

    try {
      ut.begin();
      Method createQuery = em.getClass().getMethod("createQuery", CriteriaDelete.class);
      Query q = (Query) createQuery.invoke(em, criteriaDelete);
      q.executeUpdate();
      ut.commit();
    } finally {
      ut.rollback();
    }
  }

  @Configuration
  public Option[] eclipseLinkConfig() {
    return options(baseOptions(), ariesJpa21(), transactionWrapper(), eclipseLink(),

    testBundleEclipseLink());
  }

}
