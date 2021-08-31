package com.study.test;

/**
 * 跨月,和当月 交易规则。先用交易系统克服焦虑。再通过对社会行业的认知赚钱
 * <p>
 * 所有的操作：
 * 止损：总金额2%。不卖的底仓一层。个股的止损6%，损失不能超过止损金的一半。
 * 止盈：每月没盈利那么久按照止损计算。有盈利，拿出盈利1/3和止损金作为新的止损，保证拿到趋势下最大盈利。
 * 加仓，减仓：按周根据止损金额变化情况调仓，但是如果有亏损达到止损额度一半的品种。要马上处理。容忍的波动最好根据趋势判断一下是要5%，6%或者7%。（两头大中间小？）
 *
 * @program: spring
 * @description
 * @author: 帰壹
 * @create: 2021-08-29 14:09
 **/
public class KyJiaoyiEgic {
	private static final int nextMothTotalCash = 210000; // 上个月金额，用于跨月与当月
	private static final int curInitMothTotalCash = 280000; // 当前月初始金额，用于跨月与当月
	private static final int curRealTotalCash = 330000; // 当前账户实际金额，用于跨月
	private static final double stopLoss = 0.02; //账户总止损,不会变化
	private static double acceptAmplitude = 0.06; //可接受震动幅度，基金，可以定义下跌8%，震荡7%，上涨6%。这是一个要判断的指标 TODO
	private static double singleLoss = acceptAmplitude + 0.01; // 单只接受振幅，后面常数越大，持仓越分散。（行情不好的时候分散，行情好集中 0(集中),0.1（震荡）,0.2（下跌））TODO

	public static void main(String[] args) {
		Type type = Type.KY; // 包括月内交易，跨越交易
		// 需要得出的结果是，仓位，需要买入多少只，每只的最大金额。止损金额就算
		if (type.equals(Type.KY)) {
			System.out.println("交易类型：" + (type.equals("ky") ? "跨月调整" : "当月调整"));
			KY.printKy();
		} else {
			System.out.println("交易类型：" + (type.equals("ky") ? "跨月调整" : "当月调整"));
			DY.printDy();
		}
	}

	private enum Type {
		KY, // 跨月计算
		DY  //当月计算
	}

	private static class DY {
		private static double dyYuLiuPer = 0.5; // 当月调整预留平滑持仓利润,不会变化
		private static double dyLessCost = 0.1; // 当月调整的最小持有仓位参数

		private static void printDy() {
			System.out.println("当月初始总金额：" + curInitMothTotalCash);
			System.out.println("当前账户总金额：" + curRealTotalCash);
			int stopLossCashFix = calcStopLossFixDy(); // 月内固定止损金额计算
			System.out.println("固定止损后总金额：" + (curInitMothTotalCash - stopLossCashFix));
			System.out.println("固定止损金额：" + stopLossCashFix);
			int stopLossCash = calcStopLossDy(stopLossCashFix); // 止损金额计算
			System.out.println("剩余止损金额：" + stopLossCash);
			int costCash = costCashDy(stopLossCash); //可以持有的金额计算
			System.out.println("可以持有总金额：" + costCash);
			int costCashPerOne = costCashPerOne(costCash);
			System.out.println("每只最多可以持有总金额：" + costCashPerOne);
			int costNum = costNum(costCash, costCashPerOne);
			System.out.println("最少需要持有只数：" + costNum);
			int stopLossVal = stopLossVal(stopLossCash);
			System.out.println("每一只最大可以亏损额度：" + stopLossVal);
		}

		private static int calcStopLossFixDy() { // 止损现金计算
			int liRun = curInitMothTotalCash - nextMothTotalCash;
			int stopLossCash;
			// 跨月交易
			if (liRun > 0) {
				stopLossCash = (int) (liRun * dyYuLiuPer + curInitMothTotalCash * (stopLoss));
				int all = (int) (stopLossCash / acceptAmplitude);
				if (all > curInitMothTotalCash) {
					stopLossCash = (int) (curInitMothTotalCash * acceptAmplitude);
				}
			} else {
				stopLossCash = (int) (curInitMothTotalCash * (stopLoss));
			}
			return stopLossCash;
		}

		private static int calcStopLossDy(int stopLossCashFix) { // 止损现金计算
			int liRun = curRealTotalCash - curInitMothTotalCash;

			int stopLossCash = stopLossCashFix + liRun;
			if (stopLossCash >= stopLossCashFix) { //表示有盈利
				// 这里要计算一下剩余止损金额，维持安全线
				int all = (int) (stopLossCash / acceptAmplitude);
				if (all > curRealTotalCash) {
					stopLossCash = (int) (curRealTotalCash * acceptAmplitude);
				}
			}
			return stopLossCash;
		}

		private static int costCashDy(int stopLossCash) { //可以持有的的钱计算
			int less = (int) (curRealTotalCash * dyLessCost);
			if (stopLossCash <= 0) {
				return less;
			} else {
				int less1 = (int) (stopLossCash / acceptAmplitude);
				return less > less1 ? less : less1;
			}
		}

		/**
		 * 每一支可以接受的持仓，添加了一个值，让每一支可以承担的风险更大。
		 * 规则：
		 * 1、一只的亏损不能超过总止损的一半
		 * 2、行情越好，持仓越集中，singleLoss值越小
		 *
		 * @param costCash
		 * @return
		 */
		private static int costCashPerOne(int costCash) { //每只可以持有的的钱计算
			return (int) ((costCash * (acceptAmplitude) / 2) / singleLoss);
		}

		/**
		 * 可以持有的只数
		 *
		 * @param costCash       总持有数
		 * @param costCashPerOne 每只最大持有数
		 * @return
		 */
		private static int costNum(int costCash, int costCashPerOne) { // 可以持有的只数
			double val = costCash / (double) costCashPerOne;
			return (int) Math.ceil(val);
		}

		/**
		 * 最大亏损是止损的一半
		 *
		 * @param stopLossCash
		 * @return
		 */
		private static int stopLossVal(int stopLossCash) { // 每一只最大亏损额度
			if (stopLossCash > 0) {
				return stopLossCash / 2;
			}
			return 0;
		}

	}

	private static class KY { //在一定周期重新计算（通常是跨月）
		private static double kyYuLiuPer = 0.3; // 跨月预留平滑持仓利润,不会变化

		private static void printKy() {
			System.out.println("上月总金额：" + nextMothTotalCash);
			System.out.println("实际总金额：" + curInitMothTotalCash);
			int stopLossCash = calcStopLoss(); // 止损金额计算
			System.out.println("止损后总金额：" + (curInitMothTotalCash - stopLossCash));
			System.out.println("当月硬止损金额：" + stopLossCash);
			int costCash = costCash(stopLossCash); //可以持有的金额计算
			System.out.println("可以持有总金额：" + costCash);
			int costCashPerOne = costCashPerOne(stopLossCash);
			System.out.println("每只最多可以持有总金额：" + costCashPerOne);
			int costNum = costNum(costCash, costCashPerOne);
			System.out.println("最少需要持有只数：" + costNum);
			int stopLossVal = stopLossVal(stopLossCash);
			System.out.println("每一只最大亏损额度：" + stopLossVal);
		}

		/**
		 * 总止损额度
		 * 规则：
		 * 1、有盈利计算止损
		 * 预留一定比例的利润作为当前月的止损，这样放大收益，平滑仓位。（通常预留利润的30%）
		 * 2、无盈利计算止损
		 * 最低止损 通常2% 不变
		 * 3、如果止损额度换算出来的持仓大于真实额度，就用满仓计算止损,最大持仓情况也要保持6%的止损
		 *
		 * @return
		 */
		private static int calcStopLoss() {
			int liRun = curInitMothTotalCash - nextMothTotalCash;
			int stopLossCash;
			// 跨月交易
			if (liRun > 0) {
				stopLossCash = (int) (liRun * kyYuLiuPer + curInitMothTotalCash * (stopLoss));
				int all = (int) (stopLossCash / acceptAmplitude);
				if (all > curInitMothTotalCash) {
					stopLossCash = (int) (curInitMothTotalCash * acceptAmplitude);
				}
			} else {
				stopLossCash = (int) (curInitMothTotalCash * (stopLoss));
			}
			return stopLossCash;
		}

		/**
		 * 可接受的振幅，风险越大。可接受的震动幅度就应该越大。积极定位6%
		 * 规则：
		 * 止损金额除以可接受震动幅度，就得出了可以持有多少仓位
		 *
		 * @param stopLossCash 止损金额
		 * @return
		 */
		private static int costCash(int stopLossCash) { //可以持有的的钱计算
			return (int) (stopLossCash / acceptAmplitude);
		}

		/**
		 * 每一支可以接受的持仓，添加了一个值，让每一支可以承担的风险更大。
		 * 规则：
		 * 1、一只的亏损不能超过总止损的一半
		 * 2、行情越好，持仓越集中，singleLoss值越小
		 *
		 * @param stopLossCash
		 * @return
		 */
		private static int costCashPerOne(int stopLossCash) { //每只可以持有的的钱计算
			return (int) ((stopLossCash / 2) / singleLoss);
		}

		/**
		 * 可以持有的只数
		 *
		 * @param costCash       总持有数
		 * @param costCashPerOne 每只最大持有数
		 * @return
		 */
		private static int costNum(int costCash, int costCashPerOne) { // 可以持有的只数
			double val = costCash / (double) costCashPerOne;
			return (int) Math.ceil(val);
		}

		/**
		 * 最大亏损是止损的一半
		 *
		 * @param stopLossCash
		 * @return
		 */
		private static int stopLossVal(int stopLossCash) { // 每一只最大亏损额度
			return stopLossCash / 2;
		}
	}


}
