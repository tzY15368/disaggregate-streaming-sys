package operators.stateful;

import com.google.protobuf.ByteString;
import exec.SerDe;
import operators.BaseOperator;
import pb.Tm;
import stateapis.MapStateAccessor;
import stateapis.ValueState;

import java.io.Serializable;
import java.util.*;

public class TimeSlidingWindow<IN,OUT> extends BaseOperator implements Serializable {
    private SerDe<IN> serde;
    private SerDe<OUT> serdeOut;
    private Window currentWindow;
    private long windowSize;
    private long slideStep;
    private ArrayList<IN> windowData;

    private MapStateAccessor someMapStateAccessor;
    private ValueState<Integer> intStateAccessor;
    public TimeSlidingWindow(Tm.OperatorConfig config, SerDe<IN> serde, SerDe<OUT> serdeOut, long windowSize, long slideStep) {
        this.serde = serde;
        this.windowSize = windowSize;
        this.slideStep = slideStep;
        this.serdeOut = serdeOut;
        this.windowData = new ArrayList<>();

        //
        someMapStateAccessor = stateDescriptorProvider.getMapStateAccessor(this, "some-map-state");
        intStateAccessor = stateDescriptorProvider.getValueStateAccessor(this, "some-int-state");
    }

    @Override
    protected void processElement(ByteString in) {

        Map m = someMapStateAccessor.value();
        if (m == null) {
            m = new HashMap();
        }

        // 隐式地从kvprovider拉取state
        Object o = m.get("999");
        Object o = m.getMany("999","888","777");

        // 本地更新state
        m.put("123",345);

        // 把本地的状态更新到kvprovider
        someMapStateAccessor.update(m);







        IN data = serde.deserialize(in);
        long timestamp = getDataTimestamp(data);
        if(currentWindow == null){
            currentWindow = new Window(timestamp,timestamp+windowSize);
        }
        if(currentWindow.isWithinWindow(timestamp)){
            windowData.add(data);
            if(trigger()){
                OUT result = UDF(data);
                ByteString output=serdeOut.serialize(result);
                sendOutput(Tm.Msg.newBuilder().setType(Tm.Msg.MsgType.DATA).setData(output));
            }
        }else{
            moveWindow();
            removeOldData();
            windowData.add(data);
        }
    }
    // A user defeined trigger for when we can use the data in the window
    // e.g. count the number of data in the window when the number of data reaches a threshold
    public boolean trigger(){
        // need to be implemented by the user
        return false;
    }
    public OUT UDF(IN data){
        // need to be implemented by the user
        return null;
    }
    public Window getCurrentWindow(){
        return this.currentWindow;
    }
    public void moveWindow(){
        long newStart = currentWindow.getStart()+slideStep;
        long newEnd = newStart+windowSize;
        currentWindow.setWindow(newStart,newEnd);
    }
    public void removeOldData(){
        Iterator<IN> iterator = windowData.iterator();
        while (iterator.hasNext()) {
            IN item = iterator.next();
            if (!currentWindow.isWithinWindow(getDataTimestamp(item))) {
                iterator.remove();
            }
        }
    }
    public long getDataTimestamp(IN data){
        // TODO: need to be implemented by the user based on the data type
        return 0;
    }
    @Override
    public void run(){
        super.run();
    }
}
