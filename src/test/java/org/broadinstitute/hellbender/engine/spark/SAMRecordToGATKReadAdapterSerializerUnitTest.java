package org.broadinstitute.hellbender.engine.spark;

import com.esotericsoftware.kryo.Kryo;
import org.apache.spark.SparkConf;
import org.apache.spark.serializer.KryoRegistrator;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import org.broadinstitute.hellbender.utils.read.SAMRecordToGATKReadAdapter;
import org.broadinstitute.hellbender.testutils.SparkTestUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SAMRecordToGATKReadAdapterSerializerUnitTest {

    public static class TestGATKRegistrator implements KryoRegistrator {
        @SuppressWarnings("unchecked")
        @Override
        public void registerClasses(Kryo kryo) {
            kryo.register(SAMRecordToGATKReadAdapter.class, new SAMRecordToGATKReadAdapterSerializer());
        }
    }

    @Test
    public void testSerializerRoundTripHeaderlessRead() {
        SparkConf conf = new SparkConf().set("spark.kryo.registrator",
                "org.broadinstitute.hellbender.engine.spark.SAMRecordToGATKReadAdapterSerializerUnitTest$TestGATKRegistrator");

        // check round trip with no header
        GATKRead read = ArtificialReadUtils.createHeaderlessSamBackedRead("read1", "1", 100, 50);
        final GATKRead roundTrippedRead = SparkTestUtils.roundTripInKryo(read, GATKRead.class, conf);
        Assert.assertEquals(roundTrippedRead, read);
    }

    @Test
    public void testChangingContigsOnHeaderlessGATKRead(){
        final SparkConf conf = new SparkConf().set("spark.kryo.registrator",
                "org.broadinstitute.hellbender.engine.spark.SAMRecordToGATKReadAdapterSerializerUnitTest$TestGATKRegistrator");
        final GATKRead read = ArtificialReadUtils.createHeaderlessSamBackedRead("read1", "1", 100, 50);
        final GATKRead roundTrippedRead = SparkTestUtils.roundTripInKryo(read, GATKRead.class, conf);
        Assert.assertEquals(roundTrippedRead, read);

        read.setPosition("2", 1);
        final GATKRead roundTrippedRead2 = SparkTestUtils.roundTripInKryo(read, GATKRead.class, conf);
        Assert.assertEquals(roundTrippedRead2, read);
    }

    @Test
    public void testTransientAttributeSerializationClearing(){
        final SparkConf conf = new SparkConf().set("spark.kryo.registrator",
                "org.broadinstitute.hellbender.engine.spark.SAMRecordToGATKReadAdapterSerializerUnitTest$TestGATKRegistrator");
        final GATKRead read = ArtificialReadUtils.createHeaderlessSamBackedRead("read1", "1", 100, 50);
        read.setTransientAttribute("test",1);
        read.setTransientAttribute("removed",2);
        read.clearTransientAttribute("removed");

        Assert.assertEquals(read.getTransientAttribute("test"), 1);
        Assert.assertNull(read.getTransientAttribute("removed"));

        // Round tripping the read should cause it to be serialized and consequently not bring the transient attributes with it
        final GATKRead roundTrippedRead = SparkTestUtils.roundTripInKryo(read, GATKRead.class, conf);
        
        Assert.assertNull(roundTrippedRead.getTransientAttribute("test"));
        Assert.assertNull(roundTrippedRead.getTransientAttribute("removed"));
    }
}
