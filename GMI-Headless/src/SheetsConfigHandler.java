import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SheetsConfigHandler {

@SerializedName("range")
@Expose
private String range;
@SerializedName("majorDimension")
@Expose
private String majorDimension;
@SerializedName("values")
@Expose
private List<List<String>> values = null;

/**
* No args constructor for use in serialization
*
*/
public SheetsConfigHandler() {
}

/**
*
* @param majorDimension
* @param values
* @param range
*/
public SheetsConfigHandler(String range, String majorDimension, List<List<String>> values) {
super();
this.range = range;
this.majorDimension = majorDimension;
this.values = values;
}

public String getRange() {
return range;
}

public void setRange(String range) {
this.range = range;
}

public SheetsConfigHandler withRange(String range) {
this.range = range;
return this;
}

public String getMajorDimension() {
return majorDimension;
}

public void setMajorDimension(String majorDimension) {
this.majorDimension = majorDimension;
}

public SheetsConfigHandler withMajorDimension(String majorDimension) {
this.majorDimension = majorDimension;
return this;
}

public List<List<String>> getValues() {
return values;
}

public void setValues(List<List<String>> values) {
this.values = values;
}

public SheetsConfigHandler withValues(List<List<String>> values) {
this.values = values;
return this;
}

}
