package com.esri.arcgis.android.samples.attributeeditor;


import com.esri.core.map.FeatureType;
import com.esri.core.map.Field;
import com.esri.core.map.Graphic;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kobexuqiang on 2016/1/5.
 */
public class FeatureLayerUtils {
    public enum FieldType
    {
        NUMBER,STRING,DECIMAL,DATE;
        public static FieldType determineFieldType(Field field) {

            if (field.getFieldType() == Field.esriFieldTypeString) {
                return FieldType.STRING;
            } else if (field.getFieldType() == Field.esriFieldTypeSmallInteger
                    || field.getFieldType() == Field.esriFieldTypeInteger) {
                return FieldType.NUMBER;
            } else if (field.getFieldType() == Field.esriFieldTypeSingle
                    || field.getFieldType() == Field.esriFieldTypeDouble) {
                return FieldType.DECIMAL;
            } else if (field.getFieldType() == Field.esriFieldTypeDate) {
                return FieldType.DATE;
            }
            return null;
        }
    }

    private static boolean isFieldValidForEditing(Field field)
    {
        int fieldType = field.getFieldType();
        return field.isEditable()&&fieldType != Field.esriFieldTypeOID
                && fieldType != Field.esriFieldTypeGeometry
                && fieldType != Field.esriFieldTypeBlob
                && fieldType != Field.esriFieldTypeRaster
                && fieldType != Field.esriFieldTypeGUID
                && fieldType != Field.esriFieldTypeXML;
    }
     public static boolean setAttribute(Map<String,Object> attrs,Graphic oldGraphic, Field field,String value,DateFormat formatter)
     {
         boolean hasValueChanged = false;
         if(FieldType.determineFieldType(field) == FieldType.STRING)
         {
             if(!value.equals(oldGraphic.getAttributeValue(field.getName())))
             {
                 attrs.put(field.getName(),value);
                 hasValueChanged = true;
             }
         }else if(FieldType.determineFieldType(field) == FieldType.NUMBER)
         {
             if(value.equals("") && oldGraphic.getAttributeValue(field.getName()) != Integer.valueOf(0))
             {
                 attrs.put(field.getName(),0);
                 hasValueChanged = true;
             }
             else
             {
                 int intValue = Integer.parseInt(value);
                 if(intValue != Integer.parseInt(oldGraphic.getAttributeValue(field.getName()).toString()))
                 {
                     attrs.put(field.getName(),intValue);
                     hasValueChanged = true;
                 }
             }
         }else if(FieldType.determineFieldType(field) == FieldType.DECIMAL)
         {
             if((value.equals("") && oldGraphic.getAttributeValue(field.getName())!=Double.valueOf(0)))
             {
                 attrs.put(field.getName(),0.0);
                 hasValueChanged = true;
             }else
             {
                 double dValue = Double.parseDouble(value);
                 if(dValue != Double.parseDouble(oldGraphic.getAttributeValue(field.getName()).toString()))
                 {
                     attrs.put(field.getName(),dValue);
                     hasValueChanged = true;
                 }
             }
         }else if(FieldType.determineFieldType(field) == FieldType.DATE)
         {
             Calendar c = Calendar.getInstance();
             long dateInMillis;
             try
             {
                 c.setTime(formatter.parse(value));
                 dateInMillis = c.getTimeInMillis();
                 if(dateInMillis != Long.parseLong(oldGraphic.getAttributeValue(field.getName()).toString()))
                 {
                     attrs.put(field.getName(),dateInMillis);
                     hasValueChanged = true;
                 }

             }catch(ParseException e)
             {

             }
         }
         return hasValueChanged;
     }
    public static String returnTypeIdFromTypeName(FeatureType[] types,String name)
    {
        for(FeatureType type : types)
        {
            if(type.getName().equals(name))
            {
                return type.getId();
            }
        }
        return null;
    }
  public static int[] createArrayOfFieldIndexes(Field[] fields)
  {
      ArrayList<Integer> list = new ArrayList<>();
      int fieldCount = 0;
      for(int i = 0 ; i < fields.length ; i++)
      {
          if(isFieldValidForEditing(fields[i]))
          {
              list.add(i);
              fieldCount++;
          }
      }
      int[] editableFieldIndexes = new int[fieldCount];
      for(int x = 0 ; x < list.size(); x++)
      {
          editableFieldIndexes[x] = list.get(x);
      }
      return editableFieldIndexes;
  }

 public static String[] createTypeNameArray(FeatureType[] types)
 {
     String[] typeNames = new String[types.length];
     int i = 0;
     for(FeatureType type : types)
     {
         typeNames[i] = type.getName();
         i++;
     }
     return typeNames;
 }
    public static HashMap<String,FeatureType> createTypeMapByValue(FeatureType[] types)
    {
        HashMap<String,FeatureType> typeMap = new HashMap<>();
        for(FeatureType type : types)
        {
            typeMap.put(type.getId(),type);
        }
          return typeMap;
    }


}
