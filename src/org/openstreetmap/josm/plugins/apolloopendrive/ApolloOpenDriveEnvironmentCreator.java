package org.openstreetmap.josm.plugins.apolloopendrive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import de.apollomasterbeuth.apolloconverter.OSMReaderSettings;
import de.apollomasterbeuth.apolloconverter.OSMStructureReader;
import de.apollomasterbeuth.apolloconverter.osm.Network;
import de.apollomasterbeuth.apolloconverter.osm.WayNode;
import de.apollomasterbeuth.apolloconverter.osm.WayNode.LinkDirection;
import de.apollomasterbeuth.apolloconverter.structure.Environment;
import de.uzl.itm.jaxb4osm.jaxb.NodeElement;
import de.uzl.itm.jaxb4osm.jaxb.OsmElement;

public class ApolloOpenDriveEnvironmentCreator {
	public static final String[] highwayTags = {"primary","secondary","tertiary","residential","service","unclassified"};

	
	public static Environment createEnvironment(OsmDataLayer layer) {
		Network network = createNetwork(layer.data.getPrimitives(x -> x.getClass() == Way.class).stream().map(x -> (Way)x).collect(Collectors.toList()), layer.data.getPrimitives(x -> x.getClass() == Node.class).stream().map(x -> (Node)x).collect(Collectors.toList()));
		return OSMStructureReader.createEnvironment(network, new OSMReaderSettings());
	}
	
	private static Network createNetwork(Collection<Way> wayElements, Collection<Node> nodeElements) 
	{
		List<de.apollomasterbeuth.apolloconverter.osm.Way> roadWays = new ArrayList<de.apollomasterbeuth.apolloconverter.osm.Way>();
		List<WayNode> wayNodes = new ArrayList<WayNode>();
		List<de.apollomasterbeuth.apolloconverter.osm.Node> trafficLightNodes = getTrafficLightNodes(nodeElements);
		
		wayElements.forEach(wayElement->{
			if (isWayRoad(wayElement)) {
				de.apollomasterbeuth.apolloconverter.osm.Way way = new de.apollomasterbeuth.apolloconverter.osm.Way();
				List<WayNode> waysTemp = new ArrayList<WayNode>();
				Long[] nodeIDs = wayElement.getNodeIds().stream().toArray(Long[]::new);
				
				for(int i = 0; i< nodeIDs.length; i++) {
					long currentId = nodeIDs[i];
					Optional<WayNode> nodeMatch = wayNodes.stream().filter(x -> x.id == currentId).findFirst();
					WayNode node = nodeMatch.isPresent() ? nodeMatch.get() : getWayNode(currentId, nodeElements);
					node.ways.add(way);

					if (!nodeMatch.isPresent()) {
						wayNodes.add(node);
					}
					
					if (i==0) {
						node.addLink(LinkDirection.STARTING, way);
					}
					if (i==wayElement.getNodes().size()-1) {
						node.addLink(LinkDirection.ENDING, way);
					}
					
					waysTemp.add(node);
				}
				way.nodes = waysTemp.toArray(new WayNode[0]);
				way.id = wayElement.getId();
				way.type = getWayType(wayElement);
				way.oneway = wayElement.isOneway() == 1;
				roadWays.add(way);
			}
		});
		
		Network network = new Network();
		network.roadWays = roadWays;
		network.roadNodes = wayNodes;
		network.trafficLightNodes = trafficLightNodes;
		return network;
	}
	
	private static boolean isWayRoad(Way way) {
		if (way.getKeys().containsKey("highway")) {
			String highwayTagValue = way.getKeys().get("highway");
			return Arrays.stream(highwayTags).anyMatch(validHighwayTag-> validHighwayTag.equals(highwayTagValue));
		}
		
		return false;
	}
	
	private static List<de.apollomasterbeuth.apolloconverter.osm.Node> getTrafficLightNodes(Collection<Node> nodeElements){
		List<de.apollomasterbeuth.apolloconverter.osm.Node> trafficLightNodes = new ArrayList<de.apollomasterbeuth.apolloconverter.osm.Node>();
		nodeElements.forEach(node->{
			node.getKeys().forEach((k,v)->{
				if ((k.equals("highway"))&&(v.equals("traffic_signals"))) {
					de.apollomasterbeuth.apolloconverter.osm.Node trafficLight = new de.apollomasterbeuth.apolloconverter.osm.Node(node.getCoor().getX(), node.getCoor().getY(), node.getId());
					trafficLightNodes.add(trafficLight);
				}
			});
		});
		return trafficLightNodes;
	}
	
	private static WayNode getWayNode(long id, Collection<Node> nodeElements) throws IllegalArgumentException {
		Optional<Node> optionalNodeElement = nodeElements.stream().filter(nodeElement->nodeElement.getId() == id).findFirst();
		if (optionalNodeElement.isPresent()) {
			Node nodeElement = optionalNodeElement.get();
			return new WayNode(nodeElement.getCoor().getX(), nodeElement.getCoor().getY(), nodeElement.getId());
		} else {
			throw new IllegalArgumentException("No node with the provided id in collection.");
		}
	}
	
	private static String getWayType(Way way) {
		if (way.getKeys().containsKey("highway")) {
			return way.getKeys().get("highway");
		} else {
			return "unknown";
		}
	}
}
