package zephyropen.device.beam;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import com.googlecode.charts4j.Color;

import zephyropen.api.ZephyrOpen;
import zephyropen.util.LogManager;

import zephyropen.util.Utils;
import zephyropen.util.google.GoogleChart;
import zephyropen.util.google.GoogleLineGraph;
import zephyropen.util.google.ScreenShot;

public class BeamScan {

	/** framework configuration */
	public static ZephyrOpen constants = ZephyrOpen.getReference();

	public final static String beamspin = "<id:beamspin>";
	public final static String beamreader = "<id:beamreader>";
	// public final static String filter = "filter";

	final String path = constants.get(ZephyrOpen.userHome) + ZephyrOpen.fs + "beam.properties";

	/** store past searches */
	Properties found = new Properties();
	Spin spin = null;
	Reader reader = null;

	/** */
	public BeamScan() {

		readProps();

		// need to go look?
		if ((spin == null) || (reader == null)) {
			Find find = new Find();
			if (spin == null) {
				spin = new Spin(find.search(beamspin));
			}
			if (reader == null) {
				reader = new Reader(find.search(beamreader));
			}
		}

		if (!spin.connect())
			constants.shutdown("can't find spin");

		if (!reader.connect())
			constants.shutdown("can't find reader");

		// re-fresh the file
		found.put(beamreader, reader.getPortName());
		found.put(beamspin, spin.getPortName());
		writeProps();

		Utils.delay(2000);
		constants.info("spin version: " + spin.getVersion());
		constants.info("read version: " + reader.getVersion());
		Utils.delay(2000);
	}

	/** */
	public void close() {
		reader.close();
		spin.close();
	}

	/** add devices that require com port mapping, not searching */
	public void readProps() {
		if (new File(path).exists()) {
			try {

				File file = new File(path);
				FileInputStream fi = new FileInputStream(file);
				found.load(fi);
				fi.close();

				Enumeration<Object> keys = found.keys();
				while (keys.hasMoreElements()) {
					String dev = (String) keys.nextElement();

					if (dev.equalsIgnoreCase(beamreader))
						reader = new Reader(found.getProperty(dev));
					if (dev.equalsIgnoreCase(beamspin))
						spin = new Spin(found.getProperty(dev));

				}
			} catch (Exception e) {
				constants.error(e.getMessage(), this);
			}
		}
	}

	/** */
	public void writeProps() {
		try {

			// write to search props
			FileWriter fw = new FileWriter(new File(path));
			found.store(fw, null); // "found on: " + new Date().toString());
			fw.close();

		} catch (Exception e) {
			constants.error(e.getMessage(), this);
		}
	}

	/** */
	public void test() {

		/* non-blocking */
		reader.test(false);
		spin.test(false);

		/* wait for both */
		while (spin.isBusy() || reader.isBusy()) {
			// constants.info("wait...");
			Utils.delay(500);
		}

		constants.info("data: " + reader.points.size());
		constants.info("spin: " + spin.getRuntime());
		constants.info("read: " + reader.getRuntime());
	}

	/** create graph */
	public void lineGraph(String txt) {

		GoogleChart chart = new GoogleLineGraph("beam", "ma", Color.BLUEVIOLET);
		for (int j = 0; j < reader.points.size(); j++)
			chart.add(String.valueOf(reader.points.get(j)));

		new ScreenShot(chart, 900, 300, "data: " + reader.points.size() + " " + txt);
	}

	/**  */
	public int[] getSlice(final int target) {

		int[] values = { 0, 0, 0, 0 };
		
		values[0] = getDataInc(target, 0);
		constants.info("x1: " + values[0] + " value: " + reader.points.get(values[0]));

		values[1] = getDataDec(target, values[0]);
		constants.info("x2: " + values[1] + " value: " + reader.points.get(values[1]));

		values[2] = getDataInc(target, reader.points.size()/2);
		constants.info("y1: " + values[2] + " value: " + reader.points.get(values[2]));
		
		values[3] = getDataDec(target, values[2]);
		constants.info("y2: " + values[3] + " value: " + reader.points.get(values[3]));
		
		return values;
	} 

	/** */
	private int getDataInc(final int target, final int start) {

		int j = start;

		//constants.info("start : " + j + " target : " + target);

		for (; j < reader.points.size() - 1; j++) {

			if (reader.points.get(j) >= target){
				// constants.info( "inc_index: " + j + " value: " + reader.points.get(j));
				break;
			}
		}

		return j;
	}	
	
	/** */
	private int getDataDec(final int target, final int start) {

		int j = start;
		// constants.info("start : " + j + " target : " + target);

		for (; j < reader.points.size() - 1; j++) {

			if (reader.points.get(j) <= target){
				// constants.info( "dec_index: " + j + " value: " + reader.points.get(j));
				break;
			}
		}

		return j;
	}
	
	
	/** */
	private int getMaxIndex(final int start, final int stop) {

		int j = start;
		int max = 0;
		int index = 0;
		
		// constants.info("getMaxIndex start: " + start);
		// constants.info("getMaxIndex stop: " + stop);
		
		for (; j < stop; j++) {
			if(reader.points.get(j) > max){
				max = reader.points.get(j);
				index = j;
			}
		}

		return index;
	}
	
	
	/** */
	private int[] getRange(final int target, final int start) {

		int j = start;
		// constants.info(j + " target : " + target);

		for (; j < reader.points.size() - 1; j++) {
			// constants.info( j + " get: " + reader.points.get(j));
			if (reader.points.get(j) >= target)
				break;
		}
		
		// create an array 
		int[] ans = new int[j];
		for(int i = 0 ; i < j ; i++)
			ans[i] = reader.points.get(i);
		
		return ans;
	}

	
	/** write report to file */
	public void log() {
		LogManager log = new LogManager();
		log.open(constants.get(ZephyrOpen.userLog) + ZephyrOpen.fs + "beam.log");
		log.append("spin version: " + spin.getVersion());
		log.append("read version: " + reader.getVersion());
		log.append("date: " + new Date().toString());
		log.append("data: " + reader.points.size());
		log.append("spin: " + spin.getRuntime());
		log.append("step: " + spin.getSteps());
		log.append("read: " + reader.getRuntime());
		for (int j = 0; j < reader.points.size(); j++)
			log.append(j+ "  " +String.valueOf(reader.points.get(j)));
		
		
		// log.append("data: " + reader.points.size());
	}

	/** test driver */
	public static void main(String[] args) {

		constants.init(args[0]);
		BeamScan scan = new BeamScan();
		
		scan.test();
		
		final int maxIndex = scan.getMaxIndex(0, scan.reader.points.size()/2);
		constants.info("max: " + scan.reader.points.get(maxIndex) + " index: " + maxIndex);
		
		final int max2Index = scan.getMaxIndex(scan.reader.points.size()/2, scan.reader.points.size());
		constants.info("max2: " + scan.reader.points.get(max2Index)  + " index: " + max2Index);
	
		constants.put("xBeam", maxIndex);
		constants.put("yBeam", max2Index);
		
		scan.log();
		scan.lineGraph("(" + maxIndex + ", " + max2Index + ")"); 
		
		int j = 100;
		for(; j < 800 ; j+=100){
			int[] slice = scan.getSlice(j);
			if (slice != null) 
				constants.info("slice: " + j + " x: " + (slice[1] - slice[0]) + " y: " + (slice[3] - slice[2]));
		}
		
		
		scan.close();
		Utils.delay(3000);
		constants.shutdown();
	}
}
