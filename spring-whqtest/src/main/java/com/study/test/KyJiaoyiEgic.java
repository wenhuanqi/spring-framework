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
	private static int curTotalCash = 190000; // 当前月当前金额
	private static int initTotalCash = 200000;// 当前月初始的总金额


	// 不变常数
	private static double acceptAmplitude = 0.06; //可接受震动幅度，tiaoZheng值是可以变化的
	private static double tiaoZheng = 0.01; // 上个月盈利可以适当减少可接受震动幅度，可以持有更多。该值不变
	private static double singleLoss = acceptAmplitude + 0.01; // 单只接受振幅
	private static double stopLoss = 0.02; //总止损,不会变化
	private static double yuLiuPer = 0.3; // 预留平滑持仓利润,不会变化

	private static String type = "ky"; // 包括月内交易，跨越交易


	public static void main(String[] args) {
		// 需要得出的结果是，仓位，需要买入多少只，每只的最大金额。止损金额就算
		System.out.println("交易类型：" + (type.equals("ky") ? "跨月调整" : "当月调整"));
		System.out.println("总金额：" + curTotalCash);
		int stopLossCash = calcStopLoss(); // 止损金额计算
		System.out.println("止损后总金额：" + (curTotalCash - stopLossCash));
		System.out.println("止损金额：" + stopLossCash);
		int costCash = costCash(stopLossCash); //可以持有的金额计算
		System.out.println("可以持有总金额：" + costCash);
		int costCashPerOne = costCashPerOne(stopLossCash);
		System.out.println("每只最多可以持有总金额：" + costCashPerOne);
		int costNum = costNum(costCash, costCashPerOne);
		System.out.println("最少需要持有只数：" + costNum);

	}

	private static int calcStopLoss() { // 止损现金计算
		int liRun = curTotalCash - initTotalCash;
		int stopLossCash;
		if (liRun >= 0) {
			stopLossCash = (int) (liRun * yuLiuPer + curTotalCash * (stopLoss));
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

}
