package operators.stateful;

import com.google.protobuf.ByteString;
import operators.BaseOperator;
import operators.OutputSender;
import pb.Tm;
import stateapis.ValueStateAccessor;
import utils.SerDe;

import java.io.Serializable;

public class SingleCountOperator extends BaseOperator implements Serializable {
    private transient ValueStateAccessor<Integer> cntAccesor;

    public SingleCountOperator(SerDe<String> out){
        super(null, out);
        this.setOpName("CountOperator");
    }

    @Override
    public void postInit() {
        cntAccesor = stateDescriptorProvider.getValueStateAccessor(this, "cnt", 0);
    }

    @Override
    protected void processElement(Tm.Msg msg, OutputSender outputSender) {
        //logger.info("cntAccesor.value() = " + cntAccesor.value());
        ByteString in = msg.getData();
        Integer cntVal = cntAccesor.value() + 1;
        cntAccesor.update(cntVal);
        String outMsg = "Count: " + (cntVal);
        ByteString bs = serdeOut.serializeOut(outMsg);
        Tm.Msg.Builder builder = Tm.Msg.newBuilder();
        builder.mergeFrom(msg);
        builder.setData(bs);
        System.out.println("SingleCountOperator: " + builder.build().getIngestTime());
        outputSender.sendOutput(builder.build());
    }
}
