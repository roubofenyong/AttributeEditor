package com.esri.arcgis.android.samples.attributeeditor;

import android.app.DatePickerDialog;
import android.content.Context;
import android.database.DataSetObserver;
import android.text.InputFilter;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.esri.core.map.FeatureSet;
import com.esri.core.map.FeatureType;
import com.esri.core.map.Field;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.HashMap;
import java.util.zip.Inflater;

/**
 * Created by kobexuqiang on 2016/1/5.
 */
public class AttributeListAdapter extends BaseAdapter {

    FeatureSet featureSet;
    private final Field[] fields;
    private final FeatureType[] types;
    private final String typeIdFieldName;
    private final Context context;
    private final LayoutInflater lInflator;
    private final int[] editableFieldIndexes;
    private final String[] typeNames;
    private final HashMap<String,FeatureType> typeMap;
    private AttributeItem[] items;
    final DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT);
    public AttributeListAdapter(Context context, Field[] fields, FeatureType[] types,String typeIdFieldName)
    {
        this.context = context;
        this.lInflator = LayoutInflater.from(context);
        this.fields = fields;
        this.types = types;
        this.typeIdFieldName = typeIdFieldName;
        this.editableFieldIndexes = FeatureLayerUtils.createArrayOfFieldIndexes(this.fields);
        this.typeNames = FeatureLayerUtils.createTypeNameArray(this.types);
        this.typeMap = FeatureLayerUtils.createTypeMapByValue(this.types);
        this.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                AttributeListAdapter.this.items = new AttributeItem[AttributeListAdapter.this.editableFieldIndexes.length];
            }
        });
    }

    @Override
    public int getCount() {
        return this.editableFieldIndexes.length;
    }

    @Override
    public Object getItem(int position) {
        int fieldIndex = this.editableFieldIndexes[position];
        AttributeItem row;
        if(items[position] == null) {
            row = new AttributeItem();
            row.setField(this.fields[fieldIndex]);
            Object value = this.featureSet.getGraphics()[0].getAttributeValue(fields[fieldIndex].getName());
            row.setValue(value);
            items[position] = row;
        }
        else
            {
                row = items[position];
            }
      return row;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View container = null;
        AttributeItem item =(AttributeItem)getItem(position);
        if(item.getField().getName().equals(this.typeIdFieldName))
        {
            container = lInflator.inflate(R.layout.item_spinner,null);
            String typeStringValue = this.typeMap.get(item.getValue().toString()).getName();
            Spinner spinner = createSpinnerViewFromArray(container,item.getField(),typeStringValue,this.typeNames);
            item.setView(spinner);
        }else if(FeatureLayerUtils.FieldType.determineFieldType(item.getField()) == FeatureLayerUtils.FieldType.DATE)
        {
            container = lInflator.inflate(R.layout.item_date,null);
            long date = Long.parseLong(item.getValue().toString());
            Button dateButton = createDateButtonFromLongValue(container,item.getField(),date);
            item.setView(dateButton);
        }else
        {
            View valueView = null;
            if(FeatureLayerUtils.FieldType.determineFieldType(item.getField()) == FeatureLayerUtils.FieldType.STRING)
            {
                container = lInflator.inflate(R.layout.item_text,null);
                valueView = createAttributeRow(container,item.getField(),item.getValue());
            }else if(FeatureLayerUtils.FieldType.determineFieldType(item.getField()) == FeatureLayerUtils.FieldType.NUMBER)
            {
                container = lInflator.inflate(R.layout.item_number,null);
                valueView = createAttributeRow(container,item.getField(),item.getValue());
            }else if(FeatureLayerUtils.FieldType.determineFieldType(item.getField()) == FeatureLayerUtils.FieldType.DECIMAL)
            {
                 container = lInflator.inflate(R.layout.item_decimal,null);
                valueView = createAttributeRow(container,item.getField(),item.getValue());
            }
            item.setView(valueView);

        }
      return container;
    }

    public void setFeatureSet(FeatureSet featureSet) {
        this.featureSet = featureSet;
    }
    private Spinner createSpinnerViewFromArray(View container,Field field,Object value,String[] values)
    {
        TextView fieldAlias = (TextView)container.findViewById(R.id.field_alias_txt);
        Spinner spinner = (Spinner)container.findViewById(R.id.field_value_spinner);
        fieldAlias.setText(field.getAlias());
        spinner.setPrompt(field.getAlias());
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this.context,android.R.layout.simple_spinner_dropdown_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setSelection(spinnerAdapter.getPosition(value.toString()));
        return spinner;
    }
    private Button createDateButtonFromLongValue(View container,Field field,long date)
    {
         TextView fieldAlias = (TextView) container.findViewById(R.id.field_alias_txt);
         Button dateButton = (Button) container.findViewById(R.id.field_date_btn);
         fieldAlias.setText(field.getAlias());
         Calendar c = Calendar.getInstance();
         c.setTimeInMillis(date);
         dateButton.setText(formatter.format(c.getTime()));
         addListenersToDateButton(dateButton);
         return dateButton;
    }
  private View createAttributeRow(View container, Field field, Object value)
  {
      TextView fieldAlias = (TextView) container.findViewById(R.id.field_alias_txt);
      EditText fieldValue = (EditText)container.findViewById(R.id.field_value_txt);
      fieldAlias.setText(field.getAlias());
      if(field.getLength() > 0)
      {
          InputFilter.LengthFilter filter = new InputFilter.LengthFilter(field.getLength());
          fieldValue.setFilters(new InputFilter[]{filter});
      }
      Log.d(AttributeEditorActivity.TAG, "value is null? =" + (value == null));
      Log.d(AttributeEditorActivity.TAG, "value=" + value);
      if(value != null)
      {
          fieldValue.setText(value.toString(), TextView.BufferType.EDITABLE);
      }else
      {
          fieldValue.setText("", TextView.BufferType.EDITABLE);
      }
      return fieldValue;
  }
    private void addListenersToDateButton(Button dateButton)
    {
        final ListOnDateSetListener listener = new ListOnDateSetListener(dateButton);
        dateButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Calendar c = Calendar.getInstance();
                formatter.setCalendar(c);
                try
                {
                Button button = (Button)v;
                c.setTime(formatter.parse(button.getText().toString()));
                }catch(ParseException e)
                {

                }
                int year = c.get(Calendar.YEAR);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dialog = new DatePickerDialog(context,listener,year,month,day);
                dialog.show();
            }
        });
    }
     class ListOnDateSetListener implements DatePickerDialog.OnDateSetListener
     {
         final Button button;
         public ListOnDateSetListener(Button button)
         {
             this.button = button;
         }

         @Override
         public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
             Calendar c = Calendar.getInstance();
             c.set(year,monthOfYear,dayOfMonth);
             button.setText(formatter.format(c.getTime()));
         }
     }



}
