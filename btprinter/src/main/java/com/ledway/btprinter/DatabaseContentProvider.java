package com.ledway.btprinter;


import com.activeandroid.Configuration;
import com.activeandroid.content.ContentProvider;
import com.ledway.btprinter.models.Prod;
import com.ledway.btprinter.models.ReceivedSample;
import com.ledway.btprinter.models.SampleMaster;
import com.ledway.btprinter.models.SampleProdLink;
import com.ledway.btprinter.models.TodoProd;

public class DatabaseContentProvider extends ContentProvider {

	@Override
	protected Configuration getConfiguration() {
		Configuration.Builder builder = new Configuration.Builder(getContext());
		builder.addModelClass(SampleMaster.class);
		builder.addModelClass(Prod.class);
		builder.addModelClass(TodoProd.class);
		builder.addModelClass(SampleProdLink.class);
		builder.addModelClass(ReceivedSample.class);
		return builder.create();
	}

}