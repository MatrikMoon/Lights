package moon.shared;

/**
 * Created by Moon on 6/30/2017.
 */

public interface BaseToggleActivity {
    void setToggle(boolean toggle);
    String getType();

    void debugData(String data);

    boolean getIsRice();
}
