package com.study.test;

/**
 * 跨月交易规则
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
	private enum Type {
		KY, // 跨月计算
		DY  //当月计算
	}

	private static int curTotalCash = 210000; // 当前月当前金额
	private static int initTotalCash = 200000;// 当前月初始的总金额


	// 不变常数
	private static double acceptAmplitude = 0.06; //可接受震动幅度，tiaoZheng值是可以变化的
	private static double tiaoZheng = 0.01; // 上个月盈利可以适当减少可接受震动幅度，可以持有更多。该值不变
	private static double singleLoss = acceptAmplitude + 0.01; // 单只接受振幅
	private static double stopLoss = 0.02; //总止损,不会变化

	private static double kyYuLiuPer = 0.3; // 跨月预留平滑持仓利润,不会变化

	private static double dyYuLiuPer = 0.5; // 当月调整预留平滑持仓利润,不会变化
	private static double dyLessCost = 0.1; // 当月调整的最小持有仓位参数


	public static void main(String[] args) {
		Type type = Type.KY; // 包括月内交易，跨越交易
		// 需要得出的结果是，仓位，需要买入多少只，每只的最大金额。止损金额就算
		if (type.equals(Type.KY)) {
			System.out.println("交易类型：" + (type.equals("ky") ? "跨月调整" : "当月调整"));
			printKy();
		} else {
			System.out.println("交易类型：" + (type.equals("ky") ? "跨月调整" : "当月调整"));
			printDy();
		}
	}

	private static void printDy() {
		System.out.println("当月总金额：" + initTotalCash);
		System.out.println("当前总金额：" + curTotalCash);
		int stopLossCashFix = calcStopLossFixDy(); // 月内固定止损金额计算
		System.out.println("固定止损后总金额：" + (initTotalCash - stopLossCashFix));
		System.out.println("固定止损金额：" + stopLossCashFix);
		int stopLossCash = calcStopLossDy(); // 止损金额计算
		System.out.println("浮动止损金额：" + stopLossCash);
		int costCash = costCashDy(stopLossCash); //可以持有的金额计算
		System.out.println("可以持有总金额：" + costCash);
		int costCashPerOne = costCashPerOneDy(stopLossCash, costCash);
		System.out.println("每只最多可以持有总金额：" + costCashPerOne);
		int costNum = costNum(costCash, costCashPerOne);
		System.out.println("最少需要持有只数：" + costNum);
		int stopLossVal = stopLossVal(stopLossCash);
		System.out.println("每一只最大亏损额度：" + stopLossVal);
	}

	private static void printKy() {
		System.out.println("上月总金额：" + initTotalCash);
		System.out.println("当前总金额：" + curTotalCash);
		int stopLossCash = calcStopLoss(); // 止损金额计算
		System.out.println("止损后总金额：" + (curTotalCash - stopLossCash));
		System.out.println("止损金额：" + stopLossCash);
		int costCash = costCash(stopLossCash); //可以持有的金额计算
		System.out.println("可以持有总金额：" + costCash);
		int costCashPerOne = costCashPerOne(stopLossCash);
		System.out.println("每只最多可以持有总金额：" + costCashPerOne);
		int costNum = costNum(costCash, costCashPerOne);
		System.out.println("最少需要持有只数：" + costNum);
		int stopLossVal = stopLossVal(stopLossCash);
		System.out.println("每一只最大亏损额度：" + stopLossVal);
	}

	private static int calcStopLossDy() { // 止损现金计算
		int liRun = curTotalCash - initTotalCash;
		int stopLossCash;
		// 当月交易
		int initStopLoss = (int) (initTotalCash * stopLoss); // 当月最大止损
		if (liRun >= 0) { // 有盈利情况
			stopLossCash = (int) (liRun * dyYuLiuPer + initStopLoss);
		} else {
			// 无盈利情况，止损额可能变为负数
			stopLossCash = initStopLoss + liRun;
			if (stopLossCash <= 0) {
				stopLossCash = 0;
			}
		}
		return stopLossCash;
	}

	private static int calcStopLossFixDy() { // 止损现金计算
		return (int) (initTotalCash * stopLoss);
	}

	private static int costCashDy(int stopLossCash) { //可以持有的的钱计算
		int liRun = curTotalCash - initTotalCash;
		if (liRun > 0) {
			return (int) (stopLossCash / (acceptAmplitude - tiaoZheng));
		} else if (liRun < 0) {
			int less = (int) (curTotalCash * dyLessCost);
			if (stopLossCash == 0) { // 已经到底线了
				return less;
			} else {
				int less1 = (int) (stopLossCash / (acceptAmplitude + tiaoZheng));
				return less > less1 ? less : less1;
			}
		} else {
			return (int) (stopLossCash / acceptAmplitude);
		}
	}

	private static int costCashPerOneDy(int stopLossCash, int costCash) { //每只可以持有的的钱计算
		if (stopLossCash == 0) {
			return (int) ((costCash * (acceptAmplitude) / 2) / singleLoss);
		} else {
			return (int) ((stopLossCash / 2) / singleLoss);
		}

	}

	private static int calcStopLoss() { // 止损现金计算
		int liRun = curTotalCash - initTotalCash;
		int stopLossCash;
		// 跨月交易
		if (liRun >= 0) {
			stopLossCash = (int) (liRun * kyYuLiuPer + curTotalCash * (stopLoss));
		} else {
			stopLossCash = (int) (curTotalCash * (stopLoss));
		}
		return stopLossCash;
	}


	private static int costCash(int stopLossCash) { //可以持有的的钱计算
		int liRun = curTotalCash - initTotalCash;
		if (liRun > 0) {
			return (int) (stopLossCash / (acceptAmplitude - tiaoZheng));
		} else if (liRun < 0) {
			return (int) (stopLossCash / (acceptAmplitude + tiaoZheng));
		} else {
			return (int) (stopLossCash / acceptAmplitude);
		}
	}

	private static int costCashPerOne(int stopLossCash) { //每只可以持有的的钱计算
		return (int) ((stopLossCash / 2) / singleLoss);
	}


	private static int costNum(int costCash, int costCashPerOne) { // 可以持有的只数
		double val = costCash / (double) costCashPerOne;
		return (int) Math.ceil(val);
	}

	private static int stopLossVal(int stopLossCash) { // 每一只最大亏损额度
		return stopLossCash / 2;
	}
}
