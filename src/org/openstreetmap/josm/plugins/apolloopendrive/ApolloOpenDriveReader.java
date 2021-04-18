package org.openstreetmap.josm.plugins.apolloopendrive;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.AbstractReader;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.plugins.apolloopendrive.xml.OpenDRIVE;
import org.openstreetmap.josm.plugins.apolloopendrive.xml.OpenDRIVE.Junction;
import org.openstreetmap.josm.plugins.apolloopendrive.xml.OpenDRIVE.Road;

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
					Relation roadRelation = createRelation("xodr:road");
					road.getObjects().forEach(object->{
						object.getObject().forEach(singleObject->{
							Relation objectRelation = createRelation("xodr:object");
							roadRelation.addMember(new RelationMember("xodr:object", objectRelation));
							setObjectRelationTags(objectRelation, "-1", singleObject.getId(), singleObject.getType(), singleObject.getSubtype());
							singleObject.getOutline().forEach(outline->{
								Way way = createWay();
								objectRelation.addMember(new RelationMember("xodr:outline", way));
								outline.getCornerGlobal().forEach(cornerGlobal ->{
									Node node = createNode(Double.parseDouble(cornerGlobal.getY()), Double.parseDouble(cornerGlobal.getX()), way);
								});
							});
							singleObject.getGeometry().forEach(geometry->{
								Way way = createWay();
								objectRelation.addMember(new RelationMember("xodr:geometry", way));
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
							Relation laneSectionRelation = createRelation("xodr:lanesection");
							roadRelation.addMember(new RelationMember("xodr:lanesection", laneSectionRelation));
							laneSection.getBoundaries().forEach(boundary->{
								boundary.getBoundary().forEach(boundary2->{
									Relation boundaryRelation = createRelation("xodr:boundary");
									laneSectionRelation.addMember(new RelationMember("xodr:boundary", boundaryRelation));
									boundary2.getGeometry().forEach(geometry->{
										geometry.getPointSet().forEach(pointSet->{
											Way way = createWay();
											setBoundaryWayTags(way, "-1", boundary2.getType());
											boundaryRelation.addMember(new RelationMember("xodr:geometry", way));
											pointSet.getPoint().forEach(point->{
												Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
											});
										});
									});
								});
							});
							laneSection.getCenter().forEach(center->{
								center.getLane().forEach(lane2->{
									Relation laneRelation = createRelation("xodr:centerlane");
									laneSectionRelation.addMember(new RelationMember("xodr:centerlane", laneRelation));
									lane2.getBorder().forEach(border->{
										Relation borderRelation = createRelation("xodr:border");
										laneRelation.addMember(new RelationMember("xodr:border", borderRelation));
										border.getGeometry().forEach(geometry->{
											geometry.getPointSet().forEach(pointSet->{
												Way way = createWay();
												setGeometryWayTags(way, "-1", geometry.getSOffset(), geometry.getX(), geometry.getY(), geometry.getZ(), geometry.getLength(), "centerLane");
												borderRelation.addMember(new RelationMember("xodr:geometry", way));
												pointSet.getPoint().forEach(point->{
													Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
												});
											});
										});
									});
								});
							});
							laneSection.getRight().forEach(right->{
								right.getLane().forEach(lane2->{
									Relation laneRelation = createRelation("xodr:rightlane");
									laneSectionRelation.addMember(new RelationMember("xodr:rightlane", laneRelation));
									lane2.getBorder().forEach(border->{
										Relation borderRelation = createRelation("xodr:border");
										laneRelation.addMember(new RelationMember("xodr:border", borderRelation));
										border.getGeometry().forEach(geometry->{
											geometry.getPointSet().forEach(pointSet->{
												Way way = createWay();
												setGeometryWayTags(way, "-1", geometry.getSOffset(), geometry.getX(), geometry.getY(), geometry.getZ(), geometry.getLength(), "border");
												borderRelation.addMember(new RelationMember("xodr:geometry", way));
												pointSet.getPoint().forEach(point->{
													Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
												});
											});
										});
									});
									lane2.getCenterLine().forEach(center->{
										center.getGeometry().forEach(geometry->{
											geometry.getPointSet().forEach(pointSet->{
												Way way = createWay();
												setGeometryWayTags(way, "-1", geometry.getSOffset(), geometry.getX(), geometry.getY(), geometry.getZ(), geometry.getLength(), "centerLine");
												laneRelation.addMember(new RelationMember("xodr:centerline", way));
												pointSet.getPoint().forEach(point->{
													Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
												});
											});
										});
									});
								});
							});
							laneSection.getLeft().forEach(left->{
								left.getLane().forEach(lane2->{
									Relation laneRelation = createRelation("xodr:leftlane");
									laneSectionRelation.addMember(new RelationMember("xodr:leftlane", laneRelation));
									lane2.getBorder().forEach(border->{
										Relation borderRelation = createRelation("xodr:border");
										laneRelation.addMember(new RelationMember("xodr:border", borderRelation));
										border.getGeometry().forEach(geometry->{
											geometry.getPointSet().forEach(pointSet->{
												Way way = createWay();
												setGeometryWayTags(way, "-1", geometry.getSOffset(), geometry.getX(), geometry.getY(), geometry.getZ(), geometry.getLength(), "border");
												borderRelation.addMember(new RelationMember("xodr:geometry", way));
												pointSet.getPoint().forEach(point->{
													Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
												});
											});
										});
									});
									lane2.getCenterLine().forEach(center->{
										center.getGeometry().forEach(geometry->{
											geometry.getPointSet().forEach(pointSet->{
												Way way = createWay();
												setGeometryWayTags(way, "-1", geometry.getSOffset(), geometry.getX(), geometry.getY(), geometry.getZ(), geometry.getLength(), "centerLine");
												laneRelation.addMember(new RelationMember("xodr:centerline", way));
												pointSet.getPoint().forEach(point->{
													Node node = createNode(Double.parseDouble(point.getY()), Double.parseDouble(point.getX()), way);
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
							Relation signalRelation = createRelation("xodr:signal");
							roadRelation.addMember(new RelationMember("xodr:signal", signalRelation));
							signal.getOutline().forEach(outline->{
								Way way = createWay();
								signalRelation.addMember(new RelationMember("xodr:outline", way));
								setSignalWayTags(way, "-1", signal.getId(), signal.getType(), signal.getLayoutType());
								outline.getCornerGlobal().forEach(cornerGlobal ->{
									Node node = createNode(Double.parseDouble(cornerGlobal.getY()), Double.parseDouble(cornerGlobal.getX()), way);
								});
							});
							signal.getSubSignal().forEach(subSignal->{
								subSignal.getCenterPoint().forEach(centerPoint->{
									Node node = createNode(Double.parseDouble(centerPoint.getY()), Double.parseDouble(centerPoint.getX()));
									signalRelation.addMember(new RelationMember("xodr:subsignal", node));
									setSubSignalNodeTags(node, "-1", signal.getId(), subSignal.getType());
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
	
	/**
	 * Creates a new OSM node with the specified coordinates.
	 * @param lat Latitude of the node.
	 * @param lon Longitude of the node.
	 * @return The newly created node.
	 */
	private Node createNode(final double lat, final double lon) {
		final Node node = new Node(new LatLon(lat, lon));
		getDataSet().addPrimitive(node);
		System.out.println("new geometry: " + lat + " " + lon);
		return node;
	}
	
	/**
	 * Creates a new OSM node with the specified coordinates and adds it to the specified way.
	 * @param lat Latitude of the node.
	 * @param lon Longitude of the node.
	 * @param way Way the node to be added to.
	 * @return The newly created node.
	 */
	private Node createNode(final double lat, final double lon, Way way) {
		final Node node =  new Node(new LatLon(lat, lon));
		getDataSet().addPrimitive(node);
		way.addNode(node);
		return node;
	}
	
	/**
	 * Creates a new OSM relation of the specified type.
	 * @param type Type of the relation. This value is set as the "type" tag.
	 * @return The newly created relation.
	 */
	private Relation createRelation(String type) {
		final Relation relation = new Relation();
		Map<String, String> map = new HashMap<String, String>();
		map.put("type", type);
		relation.setKeys(map);
		getDataSet().addPrimitive(relation);
		return relation;
	}
	
	/**
	 * Creates a new OSM way.
	 * @return The newly created way.
	 */
	private Way createWay() {
		final Way way = new Way();
		getDataSet().addPrimitive(way);
		return way;
	}
	
	/**
	 * Sets the tags of an OpenDRIVE SubSignal record to the specified node.
	 * @param node The node for the tags to be added to.
	 * @param id The ID used by OSM/JOSM to uniquely identify the node.
	 * @param uid "id" attribute of the SubSignal record.
	 * @param type "type" attribute of the SubSignal record.
	 */
	private void setSubSignalNodeTags(Node node, String id, String uid, String type) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("id", id);
		map.put("xodr:uid", uid);
		map.put("xodr:type", type);
		map.put("xodr:element", "subsignal");
		node.setKeys(map);
	}
	
	/**
	 * Sets the tags of an OpenDRIVE Signal record to the specified way.
	 * @param way The way for the tags to be added to.
	 * @param id The ID used by OSM/JOSM to uniquely identify the way.
	 * @param uid "id" attribute of the Signal record.
	 * @param type "type" attribute of the Signal record.
	 * @param layoutType "layoutType" attribute of the Signal record. 
	 */
	private void setSignalWayTags(Way way, String id, String uid, String type, String layoutType) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("id", id);
		map.put("xodr:uid", uid);
		map.put("xodr:type", type);
		map.put("xodr:layouttype", layoutType);
		map.put("xodr:element", "signal");
		way.setKeys(map);
	}
	
	/**
	 * Sets the tags of an OpenDRIVE Geometry record to the specified way.
	 * @param way The way for the tags to be added to.
	 * @param id The ID used by OSM/JOSM to uniquely identify the way.
	 * @param sOffset "sOffset" attribute of the Geometry record.
	 * @param x
	 * @param y
	 * @param z
	 * @param length
	 * @param parent
	 */
	private void setGeometryWayTags(Way way, String id, String sOffset, String x, String y, String z, String length, String parent) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("id", id);
		map.put("xodr:soffset", sOffset);
		map.put("xodr:x", x);
		map.put("xodr:y", y);
		map.put("xodr:z", z);
		map.put("xodr:length", length);
		map.put("xodr:element", parent);
		way.setKeys(map);
	}
	
	private void setJunctionWayTags(Way way, String uid) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("uid", uid);
		map.put("xodr:element", "junctionArea");
		way.setKeys(map);
	}
	
	/**
	 * Sets the tags of an OpenDRIVE Object record to the specified relation.
	 * @param relation The relation for the tags to be added to.
	 * @param id The ID used by OSM/JOSM to uniquely identify the relation.
	 * @param uid "id" attribute of the Object record.
	 * @param type "type" attribute of the Object record.
	 * @param subType "subType" attribute of the Object record.
	 */
	private void setObjectRelationTags(Relation relation, String id, String uid, String type, String subType) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("id", id);
		map.put("xodr:uid", uid);
		map.put("xodr:type", type);
		map.put("xodr:subtype", subType);
		map.put("xodr:element", "object");
		relation.setKeys(map);
	}
	
	/**
	 * Sets the tags of an OpenDRIVE Lane Boundary record to the specified way.
	 * @param way The way for the tags to be added to.
	 * @param id The ID used by OSM/JOSM to uniquely identify the way.
	 * @param type "type" attribute of the Lane Boundary record.
	 */
	private void setBoundaryWayTags(Way way, String id, String type) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("id", id);
		map.put("xodr:type", type);
		map.put("xodr:element", "boundary");
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
	
	
	@Override
	protected DataSet doParseDataSet(InputStream input, ProgressMonitor progressMonitor) throws IllegalDataException {
		parse(input, progressMonitor);
		return getDataSet();
	}
	
	public static DataSet parseDataSet(InputStream input, ProgressMonitor progressMonitor) throws IllegalDataException {
		return new ApolloOpenDriveReader().doParseDataSet(input, progressMonitor);
	}

}
