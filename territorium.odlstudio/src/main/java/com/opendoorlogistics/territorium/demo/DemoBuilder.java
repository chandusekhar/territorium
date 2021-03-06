/*******************************************************************************
 * Copyright (c) 2014, 2017 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.territorium.demo;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.opendoorlogistics.api.ODLApi;
import com.opendoorlogistics.api.Tables;
import com.opendoorlogistics.api.tables.ODLDatastore;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.beans.BeanTableMapping;
import com.opendoorlogistics.territorium.TerritoriumConfig;
import com.opendoorlogistics.territorium.ODLBeanCluster;
import com.opendoorlogistics.territorium.ODLBeanCustomer;
import com.opendoorlogistics.territorium.Utils;
import com.opendoorlogistics.territorium.demo.DemoAddresses.DemoLatLong;

public class DemoBuilder {
	private final ODLApi api;
	private final DemoConfig demoConfig;
	private final TerritoriumConfig config;
	private final ODLDatastore<? extends ODLTable> ioDb;
	private final DemoAddresses addresses;
	
	public DemoBuilder(ODLApi api,DemoConfig demoConfig, TerritoriumConfig capClusterConfig, ODLDatastore<? extends ODLTable> ioDb) {
		this.api = api;
		this.demoConfig = demoConfig;
		this.config = capClusterConfig;
		this.ioDb = ioDb;
		

		addresses =DemoAddresses.DEMO_ADDRESSES.get(demoConfig.country);

	}
	


	public void build(){
		Random random = new Random();
		int ncust = Math.min(addresses.size(), demoConfig.nbCustomers);
		ArrayList<Integer> indices = new ArrayList<>();
		for(int i =0 ; i <addresses.size(); i++){
			indices.add(i);
		}
		Collections.shuffle(indices, random);
	
		ODLTable customersTable = ioDb.getTableAt(0);
		Tables tables= api.tables();
		tables.clearTable(customersTable);
		double sumQuantity=0;
		
		List<ODLBeanCustomer> customers = new ArrayList<>();
		for(int i =0 ; i <ncust ; i++){
			ODLBeanCustomer customer = new ODLBeanCustomer();
			customer.setId(Integer.toString(i+1));
			customer.setCostPerUnitTravelTime(1);
			customer.setCostPerUnitTravelDistance(0);
			DemoLatLong ll = addresses.position(indices.get(i));
			customer.setLatitude(ll.getLatitude());
			customer.setLongitude(ll.getLongitude());
			double quantity = 1+ Math.round(random.nextDouble()*99);
			customer.setQuantity(quantity);
			sumQuantity += quantity;
			customers.add(customer);
		//	btm.writeObjectToTable(customer, customersTable);
		}
		
		if(config.isUseInputClusterTable()){
			ODLTable clustersTable = ioDb.getTableAt(1);
			BeanTableMapping btm = tables.mapBeanToTable(ODLBeanCluster.class);
			tables.clearTable(clustersTable);
			if(demoConfig.nbClusters>0){
				double meanQuantity = sumQuantity / demoConfig.nbClusters;
				for(int i =0 ; i<demoConfig.nbClusters ; i++){
					ODLBeanCluster cluster = new ODLBeanCluster();
					cluster.setId(Integer.toString(i+1));
					cluster.setDisplayColour(Utils.PREDEFINED_COLOURS_BY_NUMBER_STRING.get(Integer.toString(i)));
					if(cluster.getDisplayColour()==null){
						cluster.setDisplayColour(Utils.randomColour(random));						
					}
					cluster.setMinQuantity(Math.floor(meanQuantity * 0.7));
					cluster.setMaxQuantity(Math.ceil(meanQuantity * 1.3));
					btm.writeObjectToTable(cluster, clustersTable);
				}
				
			}
		
		}else{
			// renormalise customer quantity so we are at 80% of available capacity
			double availableCapacity = config.getNumberClusters() * config.getMaxClusterQuantity();
			double factor = availableCapacity / sumQuantity;
			customers.forEach(c->c.setQuantity(Math.round(c.getQuantity()*factor)));
		}
		
		// write customers to table
		BeanTableMapping btm = tables.mapBeanToTable(ODLBeanCustomer.class);		
		customers.forEach(c->btm.writeObjectToTable(c, customersTable));

	}
}
