package services;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import data.DataItem;
import data.SimpleDataItem;
import pollable.Completable;
import pollable.Pollable;
import utils.Errors;
import java.util.ArrayList;


public class ScheduledOperation extends Pollable implements Completable
{
	protected boolean completed = false;
	protected Service operation = null;
	
	protected boolean success = false;
	
	protected DataItem dataItem = null;
	
	//new code for tracking
	protected ArrayList<String> tracking_paths;
	protected int storage_level;

	//end code

	public ScheduledOperation(Service op) { 
		operation = op; completed = false; dataItem = null; success = false; 

		tracking_paths= new ArrayList<String>();
		storage_level = 0;
	}
	
	public synchronized boolean isReady() { return completed; }

	public synchronized void setPaths_Level(ArrayList<Integer> _t, int _level){
		
		for (Integer i: _t) {
			tracking_paths.add(String.valueOf(i));
		}


		storage_level = _level;
	}

	public synchronized ArrayList<Integer> getPathOrder(){
		waitUntilReady();
		ArrayList<Integer> res_tracking_paths= new ArrayList<Integer>();

		for(String str_i : tracking_paths ){
			res_tracking_paths.add(Integer.valueOf(str_i));
		}

		return res_tracking_paths;
	}

	public synchronized int getStorageLevel(){
		waitUntilReady();
		return storage_level;
	}

	public synchronized void onSuccess(DataItem d) 
	{
		completed = true;
		success = true;
		dataItem = d;
	}
	

	public synchronized void onFailure()
	{
		completed = true;
		success = false;
		dataItem = null;
		
		Errors.error("CODING FAIL!");
	}

	public synchronized DataItem getDataItem()
	{
		waitUntilReady();
		return dataItem;
	}

	public Service getOperation() { return operation; }

	public synchronized boolean wasSuccessful() // will block until ready
	{
		if(completed == false) { waitUntilReady(); }
		return success;
	}

	public void save(ObjectOutputStream os) throws Exception
	{
		os.writeBoolean(completed);
		
		// save op (workaround for ObliviStore) // TODO: do things correctly later
		//os.writeObject(operation);
		boolean operationNotNull = operation != null;
		os.writeBoolean(operationNotNull);
		
		if(operationNotNull == true)
		{
			os.writeLong(operation.reqId);
			os.writeLong(operation.opId);
			os.writeObject(operation.key);
		}
		
		os.writeBoolean(success);
		
		Errors.verify(dataItem != null);
		os.writeObject(dataItem.toString());

		//write path
		//ArrayList<String> sNumbers = new ArrayList<String>();
		//for (Integer i: tracking_paths) {
		//	sNumbers.add(String.valueOf(i));
		//}
		os.writeObject(tracking_paths);

		//write int level
		os.writeInt(storage_level);
	}
	
	public ScheduledOperation(ObjectInputStream is) throws Exception
	{
		completed = is.readBoolean();
		
		// load op (workaround for ObliviStore) // TODO: do things correctly later
		//operation = (Operation) is.readObject();
		
		operation = null;
		boolean operationNotNull = is.readBoolean();
		
		if(operationNotNull == true)
		{
			long reqId = is.readLong();
			long opId = is.readLong();
			String key = (String) is.readObject();		
			operation = new DownloadObject(reqId, opId, key);
		}
		
		success = is.readBoolean();
		
		String fromString = (String)is.readObject();
		dataItem = new SimpleDataItem(fromString);

		//read path
		tracking_paths = new ArrayList<String>();
		tracking_paths = (ArrayList<String>) is.readObject();

		

		//read int level
		storage_level = is.readInt();
	}
}