package org.openstreetmap.josm.plugins.apolloopendrive;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.AbstractReader;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.plugins.apolloopendrive.xml.OpenDRIVE;
import org.openstreetmap.josm.plugins.apolloopendrive.xml.OpenDRIVE.Junction;
import org.openstreetmap.josm.plugins.apolloopendrive.xml.OpenDRIVE.Road;

import sun.util.logging.resources.logging;

public class ApolloOpenDriveReader extends AbstractReader {

	private OpenDRIVE readOpenDriveData(InputStream input) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(OpenDRIVE.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		OpenDRIVE data = (OpenDRIVE) jaxbUnmarshaller.unmarshal(input);
		
		return data;
	}
	
	private void parse(InputStream input, ProgressMonitor progressMonitor) {
		try {
			OpenDRIVE data = readOpenDriveData(input);
			progressMonitor.setTicksCount(data.getLinkOrGeometryOrOutline().size());
			for (int i = 0; i< data.getLinkOrGeometryOrOutline().size(); i++) {
				Object item = data.getLinkOrGeometryOrOutline().get(i);
				progressMonitor.setTicks(i);
				Class<? extends Object> className = item.getClass();
				if (className==org.openstreetmap.josm.plugins.apolloopendrive.xml.OpenDRIVE.Junction.class) {
					Junction junction = (Junction)item;
					junction.getOutline().forEach(outline->{
						String id = junction.getId();
						Way way = createWay();
						setJunctionWayTags(way, id);
						outline.getCornerGlobal().forEach(cornerGlobal->{
							Node node = createNode(Double.parseDouble(cornerGlobal.getY()), Double.parseDouble(cornerGlobal.getX()), way);
						});
					});
				}
				if (className==org.openstreetmap.josm.plugins.apolloopendrive.xml.OpenDRIVE.Road.class) {
					Road road = (Road)item;
					road.getObjects().forEach(object->{
						object.getObject().forEach(singleObject->{
							singleObject.getOutline().forEach(outline->{
								Way way = createWay();
								setWayTags(way, singleObject.getId(), "outline", "0", singleObject.getType(), "false", road.getId(), "objectOutline");
								outline.getCornerGlobal().forEach(cornerGlobal ->{
									Node node = createNode(Double.parseDouble(cornerGlobal.getY()), Double.parseDouble(cornerGlobal.getX()), way);
								});
							});
							singleObject.getGeometry().forEach(geometry->{
								Way way = createWay();
								setWayTags(way, singleObject.getId(), "all", "0", singleObject.getType(), "false", road.getId(), "objectGeometry");
								geometry.getPointSet().forEach(pointSet ->{
									pointSet.getPoint().forEach(point->{
										Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
									});
								});
							});
						});
					});
					road.getLanes().forEach(lane->{
						lane.getLaneSection().forEach(laneSection->{
							laneSection.getBoundaries().forEach(boundary->{
								boundary.getBoundary().forEach(boundary2->{
									boundary2.getGeometry().forEach(geometry->{
										geometry.getPointSet().forEach(pointSet->{
											Way way = createWay();
											setWayTags(way, "-1" , "boundary", "0", boundary2.getType(), laneSection.getSingleSide(), road.getId(), "boundary");
											pointSet.getPoint().forEach(point->{
												Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
												//System.out.println(point.getY()+" "+point.getX());
											});
										});
									});
								});
							});
							laneSection.getCenter().forEach(center->{
								center.getLane().forEach(lane2->{
									lane2.getBorder().forEach(border->{
										border.getGeometry().forEach(geometry->{
											Node geometryNode = createNode(Double.parseDouble(geometry.getY()),Double.parseDouble(geometry.getX()));
											setGeometryNodeTags(geometryNode, road.getId(), lane2.getId());
											geometry.getPointSet().forEach(pointSet->{
												Way way = createWay();
												setWayTags(way, lane2.getUid(), "center", lane2.getId(), "centerLane", laneSection.getSingleSide(), road.getId(), lane2.getDirection());
												pointSet.getPoint().forEach(point->{
													Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
													//System.out.println(point.getY()+" "+point.getX());
												});
											});
										});
									});
								});
							});
							laneSection.getRight().forEach(right->{
								right.getLane().forEach(lane2->{
									lane2.getBorder().forEach(border->{
										border.getGeometry().forEach(geometry->{
											Node geometryNode = createNode(Double.parseDouble(geometry.getY()),Double.parseDouble(geometry.getX()));
											setGeometryNodeTags(geometryNode, road.getId(), lane2.getId());
											geometry.getPointSet().forEach(pointSet->{
												Way way = createWay();
												setWayTags(way, lane2.getUid(), "right", lane2.getId(), "border", laneSection.getSingleSide(), road.getId(), lane2.getDirection());
												pointSet.getPoint().forEach(point->{
													Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
													//System.out.println(point.getY()+" "+point.getX());
												});
											});
										});
									});
									lane2.getCenterLine().forEach(center->{
										center.getGeometry().forEach(geometry->{
											Node geometryNode = createNode(Double.parseDouble(geometry.getY()),Double.parseDouble(geometry.getX()));
											setGeometryNodeTags(geometryNode, road.getId(), lane2.getId());
											geometry.getPointSet().forEach(pointSet->{
												Way way = createWay();
												setWayTags(way, lane2.getUid(), "right", lane2.getId(), "center", laneSection.getSingleSide(), road.getId(), lane2.getDirection());
												pointSet.getPoint().forEach(point->{
													Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
													//System.out.println(point.getY()+" "+point.getX());
												});
											});
										});
									});
								});
							});
						});
					});
					road.getSignals().forEach(signals->{
						signals.getSignal().forEach(signal->{
							signal.getOutline().forEach(outline->{
								Way way = createWay();
								setWayTags(way, signal.getId(), "outline", "0", signal.getType(), "false", road.getId(), "signal");
								outline.getCornerGlobal().forEach(cornerGlobal ->{
									Node node = createNode(Double.parseDouble(cornerGlobal.getY()), Double.parseDouble(cornerGlobal.getX()), way);
								});
							});
							signal.getSubSignal().forEach(subSignal->{
								subSignal.getCenterPoint().forEach(centerPoint->{
									Node node = createNode(Double.parseDouble(centerPoint.getY()), Double.parseDouble(centerPoint.getX()));
									setSignalNodeTags(node, signal.getId(), subSignal.getId());
								});
							});
						});
					});
				}
			}
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
	
	private Node createNode(final double lat, final double lon) {
		final Node node = new Node(new LatLon(lat, lon));
		getDataSet().addPrimitive(node);
		System.out.println("new geometry: " + lat + " " + lon);
		return node;
	}
	
	private Node createNode(final double lat, final double lon, Way way) {
		final Node node =  new Node(new LatLon(lat, lon));
		getDataSet().addPrimitive(node);
		way.addNode(node);
		return node;
	}
	
	private void setSignalNodeTags(Node node, String uid, String id) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("uid", uid);
		map.put("id", id);
		
		node.setKeys(map);
	}
	
	private void setGeometryNodeTags(Node node, String uid, String lane) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("uid", uid);
		map.put("lane", lane);
		
		node.setKeys(map);
	}
	
	private void setJunctionWayTags(Way way, String uid) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("uid", uid);
		map.put("xodr:type", "junctionArea");
		way.setKeys(map);
	}
	
	private void setWayTags(Way way, String uid, String lane, String order, String type, String singleSide, String road, String direction) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("uid", uid);
		map.put("lane", lane);
		map.put("order", order);
		map.put("xodr:type", type);
		map.put("oneway", singleSide);
		map.put("road", road);
		map.put("direction", direction != null ? direction : "null");
		System.out.println(way.getId());
		System.out.println(uid);
		System.out.println(lane);
		System.out.println(order);
		System.out.println(type);
		way.setKeys(map);
	}
	
	private Way createWay() {
		final Way way = new Way();
		getDataSet().addPrimitive(way);
		return way;
	}
	
	@Override
	protected DataSet doParseDataSet(InputStream input, ProgressMonitor progressMonitor) throws IllegalDataException {
		parse(input, progressMonitor);
		return getDataSet();
	}
	
	public static DataSet parseDataSet(InputStream input, ProgressMonitor progressMonitor) throws IllegalDataException {
		return new ApolloOpenDriveReader().doParseDataSet(input, progressMonitor);
	}

}
