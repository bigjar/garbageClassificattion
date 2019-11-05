package garbageClassfication;

import com.alibaba.tianchi.garbage_image_util.*;
import com.intel.analytics.zoo.pipeline.inference.AbstractInferenceModel;
import com.intel.analytics.zoo.pipeline.inference.JTensor;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

import java.io.*;
import java.util.List;
import java.util.*;

import static garbageClassfication.ImagePreProcess.byteArrayToFloatArray;

public class StreamingJob {

	public static void main(String[] args) throws Exception {
		int[] inputShape = {1, 224, 224, 3};
		String input = "input_1";
		float scale = 1.0f;
		String savedModelPath = System.getenv(ConfigConstant.IMAGE_MODEL_PACKAGE_PATH);
		boolean ifReverseInputChannels = true;
		long fileSize = new File(savedModelPath).length();
		InputStream inputStream = new FileInputStream(savedModelPath);
		byte[] savedModelBytes = new byte[(int)fileSize];
		inputStream.read(savedModelBytes);
		float[] meanValues = {123.68f, 116.78f, 103.94f};
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		ImageDirSource source = new ImageDirSource();
		env.addSource(source).setParallelism(1)
				.map(new ProcessMapFunction()).setParallelism(2)
				.flatMap(new InferenceFlatMapFunction(savedModelBytes, inputShape, ifReverseInputChannels, meanValues, scale, input)).setParallelism(2)
				.addSink(new ImageClassSink()).setParallelism(1);
		env.execute("Flink Streaming Java API Skeleton");
	}

	public static class GarbageClassificationInferenceModel extends AbstractInferenceModel {
		public GarbageClassificationInferenceModel(int concurrentNum) {
			super(concurrentNum);
		}
	}

	public static class ProcessMapFunction implements MapFunction<ImageData, Tuple2<String, JTensor>> {
		int[] inputShape = {1, 224, 224, 3};

		@Override
		public Tuple2<String, JTensor> map(ImageData value) throws Exception {
			float[] imageData = byteArrayToFloatArray(value.getImage());
			JTensor jTensor = new JTensor(imageData, inputShape);
			return new Tuple2<>(value.getId(), jTensor);
		}
	}

	public static class InferenceFlatMapFunction extends RichFlatMapFunction<Tuple2<String, JTensor>, IdLabel> {
		private GarbageClassificationInferenceModel model;
		private byte[] savedModelBytes;
		private int[] inputShape;
		private boolean ifReverseInputChannels;
		private float[] meanValues;
		private float scale;
		private String input;

		public InferenceFlatMapFunction(byte[] savedModelBytes,
										int[] inputShape, boolean ifReverseInputChannels,
										float[] meanValues, float scale, String input) {
			this.savedModelBytes = savedModelBytes;
			this.inputShape = inputShape;
			this.ifReverseInputChannels = ifReverseInputChannels;
			this.input = input;
			this.meanValues = meanValues;
			this.scale = scale;
		}

		@Override
		public void open(Configuration parameters) throws Exception {
			model = new GarbageClassificationInferenceModel(1);
			model.loadTF(savedModelBytes, inputShape, ifReverseInputChannels, meanValues, scale, input);
		}

		@Override
		public void close() throws Exception {
			model.release();
		}

		@Override
		public void flatMap(Tuple2<String, JTensor> value, Collector<IdLabel> out) throws Exception {
			List<JTensor> data = Arrays.asList(value.f1);
			List<List<JTensor>> inputs = new ArrayList<>();
			inputs.add(data);
			float[] outputData = model.predict(inputs).get(0).get(0).getData();
			int index = indexOfMax(outputData);
			String label = indexMap.get(index);
			System.out.println("imageId: " + value.f0 + " label:" + label);
			IdLabel idLabel = new IdLabel(value.f0, label);
			out.collect(idLabel);
		}
	}

	public static int indexOfMax(float[] array) {
		int maxAt = 0;
		for (int i = 1; i < array.length; i++) {
			maxAt = array[i] > array[maxAt] ? i : maxAt;
		}
		return maxAt;
	}

	public static Map<Integer, String> indexMap = new HashMap<Integer, String>(){{
		put(26, "奶粉");
		put(75, "纸箱");
		put(79, "胶水");
		put(16, "吹风机");
		put(20, "塑料玩具");
		put(53, "椅子");
		put(7, "充电器");
		put(23, "塑料袋");
		put(73, "纸尿裤");
		put(62, "牙刷");
		put(11, "剃须刀");
		put(90, "辣椒");
		put(17, "土豆");
		put(66, "瓶盖");
		put(1, "一次性塑料手套");
		put(38, "抹布");
		put(49, "杏核");
		put(10, "充电线");
		put(22, "塑料盖子");
		put(28, "干电池");
		put(61, "烟盒");
		put(4, "中性笔");
		put(46, "旧镜子");
		put(8, "充电宝");
		put(99, "鼠标");
		put(55, "水彩笔");
		put(85, "蒜皮");
		put(45, "旧玩偶");
		put(92, "退热贴");
		put(30, "废弃食用油");
		put(96, "青椒");
		put(15, "口服液瓶");
		put(3, "一次性纸杯");
		put(76, "纽扣");
		put(40, "指甲油瓶子");
		put(42, "插座");
		put(9, "充电电池");
		put(19, "塑料桶");
		put(89, "袜子");
		put(67, "电视机");
		put(35, "护手霜");
		put(32, "手表");
		put(72, "红豆");
		put(88, "衣架");
		put(60, "消毒液瓶");
		put(14, "医用棉签");
		put(34, "扫把");
		put(59, "海绵");
		put(18, "塑料包装");
		put(81, "菜刀");
		put(87, "蛋_蛋壳");
		put(12, "剪刀");
		put(47, "暖宝宝贴");
		put(74, "纸巾_卷纸_抽纸");
		put(71, "糖果");
		put(94, "铅笔屑");
		put(25, "头饰");
		put(57, "泡沫盒子");
		put(33, "打火机");
		put(48, "杀虫剂");
		put(54, "毛毯");
		put(80, "自行车");
		put(77, "耳机");
		put(6, "信封");
		put(93, "酸奶盒");
		put(5, "作业本");
		put(39, "拖把");
		put(24, "外卖餐盒");
		put(56, "水龙头");
		put(44, "旧帽子");
		put(84, "蒜头");
		put(69, "白糖_盐");
		put(86, "蚊香");
		put(31, "快递盒");
		put(78, "胶带");
		put(82, "菜板");
		put(37, "抱枕");
		put(58, "洗面奶瓶");
		put(70, "空调机");
		put(29, "废弃衣服");
		put(97, "面膜");
		put(98, "香烟");
		put(43, "无纺布手提袋");
		put(0, "PET塑料瓶");
		put(27, "姜");
		put(36, "护肤品玻璃罐");
		put(91, "过期化妆品");
		put(95, "陶瓷碗碟");
		put(13, "化妆品瓶");
		put(52, "棉签");
		put(41, "指甲钳");
		put(65, "牛奶盒");
		put(63, "牙签");
		put(21, "塑料盆");
		put(83, "葡萄干");
		put(51, "果皮");
		put(64, "牙膏皮");
		put(2, "一次性筷子");
		put(68, "电风扇");
		put(50, "杯子");
	}};

}
