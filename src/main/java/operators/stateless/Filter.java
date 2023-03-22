package operators.stateless;

import java.io.Serializable;
import java.util.function.Predicate;

import com.google.protobuf.ByteString;
import utils.SerDe;
import pb.Tm;
import operators.BaseOperator;

public class Filter<T> extends BaseOperator implements Serializable {
    private Predicate<T> predicate;
    private SerDe<T> serde;
    public Filter(SerDe<T> serde) {
        this.setName("Filter-");
        this.predicate = UDFpredicate;
        this.serde = serde;
    }
    @Override
    public void run(){
        super.run();
    }
    @Override
    protected void processElement(ByteString in) {
        T data = serde.deserialize(in);
        if (predicate.test(data)) {
            sendOutput(Tm.Msg.newBuilder().setType(Tm.Msg.MsgType.DATA).setData(in));
        }
    }

    // need to be implemented by the user
    Predicate<T> UDFpredicate = new Predicate<T>() {
        @Override
        public boolean test(T t) {
            return false;
        }
    };



}