package com.esri.arcgis.android.samples.attributeeditor;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.esri.android.map.FeatureLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.FeatureEditError;
import com.esri.core.map.FeatureEditResult;
import com.esri.core.map.FeatureSet;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.tasks.SpatialRelationship;
import com.esri.core.tasks.ags.query.Query;

import java.util.HashMap;
import java.util.Map;

public class AttributeEditorActivity extends Activity {
    private MapView mapView;
    private ArcGISFeatureLayer featureLayer;
    private ArcGISDynamicMapServiceLayer operationalLayer;
    private Point pointClicked;
    private Envelope initextent;
    private LayoutInflater inflator;
    private AttributeListAdapter listAdapter;
    private ListView listView;
    private View listLayout;
    public static final String TAG = "AttributeEditorSample";
    private static final int ATTRIBUTE_EDITOR_DIALOG_ID = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapView = new MapView(this);
        initextent = new Envelope(-10868502.895856911,4470034.144641369,-10837928.084542884,4492965.25312689);
        mapView.setExtent(initextent, 0);
        setContentView(mapView);
        ArcGISTiledMapServiceLayer basemap = new ArcGISTiledMapServiceLayer(getResources().getString(R.string.basemap));
        mapView.addLayer(basemap);
        operationalLayer = new ArcGISDynamicMapServiceLayer(getResources().getString(R.string.operational_layer));
        mapView.addLayer(operationalLayer);
        featureLayer = new ArcGISFeatureLayer(getResources().getString(R.string.feature_layer), ArcGISFeatureLayer.MODE.SELECTION);
        mapView.addLayer(featureLayer);
        SimpleFillSymbol sfs = new SimpleFillSymbol(Color.TRANSPARENT);
        sfs.setOutline(new SimpleLineSymbol(Color.YELLOW, 5));
        featureLayer.setSelectionSymbol(sfs);
        inflator = LayoutInflater.from(getApplicationContext());
        listLayout = inflator.inflate(R.layout.list_layout,null);
        listView = (ListView)listLayout.findViewById(R.id.list_view);
        if(featureLayer.isInitialized())
        {
            listAdapter = new AttributeListAdapter(this,featureLayer.getFields(),featureLayer.getTypes(),featureLayer.getTypeIdField());
        }else
        {
            featureLayer.setOnStatusChangedListener(new OnStatusChangedListener()
            {
                private static final long serialVersionUID = 1L;
                @Override
                public void onStatusChanged(Object o, STATUS status) {
                        if(status == STATUS.INITIALIZED)
                        {
                            listAdapter = new AttributeListAdapter(AttributeEditorActivity.this,featureLayer.getFields(),featureLayer.getTypes(),featureLayer.getTypeIdField());
                        }
                }
            });
        }
        mapView.setOnSingleTapListener(new OnSingleTapListener() {
            private static final long serialVersionUID = 1L;
            @Override
            public void onSingleTap(float x, float y) {
                pointClicked = mapView.toMapPoint(x,y);
                Query query = new Query();
                query.setOutFields(new String[]{"*"});
                query.setSpatialRelationship(SpatialRelationship.INTERSECTS);
                query.setGeometry(pointClicked);
                query.setInSpatialReference(mapView.getSpatialReference());
                featureLayer.selectFeatures(query,ArcGISFeatureLayer.SELECTION_METHOD.NEW, new CallbackListener<FeatureSet>(){
                    @Override
                    public void onCallback(FeatureSet queryResults) {
                       if(queryResults.getGraphics().length > 0)
                       {
                           Log.d(TAG,"Feature found id = "+queryResults.getGraphics()[0].getAttributeValue(featureLayer.getObjectIdField()));
                           listAdapter.setFeatureSet(queryResults);
                           listAdapter.notifyDataSetChanged();
                           AttributeEditorActivity.this.runOnUiThread(new Runnable() {
                               @Override
                               public void run() {
                                   showDialog(ATTRIBUTE_EDITOR_DIALOG_ID);
                               }
                           });
                       }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.d(TAG,"Select Features Error"+ throwable.getLocalizedMessage());
                    }
                });
            }
        });
    }

    protected Dialog onCreateDialog(int id)
    {
        switch(id)
        {
            case ATTRIBUTE_EDITOR_DIALOG_ID:
                Dialog dialog = new Dialog(this);
                listView.setAdapter(listAdapter);
                dialog.setContentView(listLayout);
                dialog.setTitle("Edit Attributes");
                Button btnEditCancel = (Button)listLayout.findViewById(R.id.btn_edit_discard);
                btnEditCancel.setOnClickListener(returnOnClickDiscardChangesListener());
                Button btnEditApply = (Button)listLayout.findViewById(R.id.btn_edit_apply);
                btnEditApply.setOnClickListener(returnOnClickApplyChangesListener());
                return dialog;
        }
        return null;
    }

      private View.OnClickListener returnOnClickApplyChangesListener()
      {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isTypeField = false;
                    boolean hasEdits = false;
                    boolean updateMapLayer = false;
                    Map<String,Object> attrs = new HashMap<>();
                    for(int i = 0 ; i < listAdapter.getCount() ; i++)
                    {
                        AttributeItem item = (AttributeItem)listAdapter.getItem(i);
                        String value;
                        if(item.getView() != null)
                        {
                            if(item.getField().getName().equals(featureLayer.getTypeIdField()))
                            {
                                Spinner spinner = (Spinner)item.getView();
                                if(item.getField().getAlias().equals("Field Status"))
                                    continue;
                                String typeName = spinner.getSelectedItem().toString();
                                value = FeatureLayerUtils.returnTypeIdFromTypeName(featureLayer.getTypes(),typeName);
                                isTypeField = true;
                            }else if(FeatureLayerUtils.FieldType.determineFieldType(item.getField()) == FeatureLayerUtils.FieldType.DATE)
                            {
                                Button dateButton = (Button)item.getView();
                                value = dateButton.getText().toString();
                            }
                            else
                            {
                                EditText editText = (EditText)item.getView();
                                value = editText.getText().toString();
                            }
                            boolean hasChanged = FeatureLayerUtils.setAttribute(attrs,listAdapter.featureSet.getGraphics()[0],item.getField(),value,listAdapter.formatter);
                            if(hasChanged)
                            {
                                Log.d(TAG,"Change found for field=" + item.getField().getName() + "value = " + value+"applyEdits() will be called");
                                hasEdits = true;
                                if(isTypeField)
                                {
                                    updateMapLayer = true;
                                }
                            }
                            if(isTypeField)
                            {
                                isTypeField = false;
                            }
                        }
                    }
                    if(hasEdits)
                    {
                        attrs.put(featureLayer.getObjectIdField(),listAdapter.featureSet.getGraphics()[0].getAttributeValue(featureLayer.getObjectIdField()));
                        Graphic newGraphic = new Graphic(null,null,attrs);
                        featureLayer.applyEdits(null,null,new Graphic[]{newGraphic},createEditCallbackListener(updateMapLayer));
                    }
                    dismissDialog(ATTRIBUTE_EDITOR_DIALOG_ID);
                }

            };

      }

    private View.OnClickListener  returnOnClickDiscardChangesListener()
    {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             dismissDialog(ATTRIBUTE_EDITOR_DIALOG_ID);
            }
        };
    }
     private CallbackListener<FeatureEditResult[][]> createEditCallbackListener(final boolean updateLayer)
     {
         return new CallbackListener<FeatureEditResult[][]>() {
             @Override
             public void onCallback(FeatureEditResult[][] result) {
                if(result[2] != null && result[2][0] != null && result[2][0].isSuccess())
                {
                    Log.d(AttributeEditorActivity.TAG,"Success updating feature with id = "+ result[2][0].getObjectId());
                    if(updateLayer)
                    {
                        operationalLayer.refresh();
                    }
                }
             }

             @Override
             public void onError(Throwable throwable) {
               Log.d(AttributeEditorActivity.TAG,"error updating feature:" + throwable.getLocalizedMessage());
             }
         };
     }

}
