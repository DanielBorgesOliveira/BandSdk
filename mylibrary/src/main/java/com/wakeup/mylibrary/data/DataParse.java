package com.wakeup.mylibrary.data;

import com.wakeup.mylibrary.Config;
import com.wakeup.mylibrary.bean.BandInfo;
import com.wakeup.mylibrary.bean.Battery;
import com.wakeup.mylibrary.bean.BloodOxygenBean;
import com.wakeup.mylibrary.bean.BloodPressureBean;
import com.wakeup.mylibrary.bean.CurrentDataBean;
import com.wakeup.mylibrary.bean.HeartRateBean;
import com.wakeup.mylibrary.bean.HourlyMeasureDataBean;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 解析蓝牙发过来的数据
 */
public class DataParse {
    private static final String TAG = DataParse.class.getSimpleName();

    private static DataParse instance;


    public static synchronized DataParse getInstance() {

        if (instance == null) {
            instance = new DataParse();
        }
        return instance;

    }

    public Object parseData(List<Integer> datas) {
        Object object = null;

        if (datas.get(0) == 0xAB) {

            switch (datas.get(4)) {
                case 0x91:
                    //电池电量
                    Battery battery = new Battery();
                    battery.setBattery(datas.get(7));
                    object = battery;
                    break;
                case 0x92:
                    BandInfo bandInfo = new BandInfo();
                    bandInfo.setFirmwareVersionCode(datas.get(6) + (float) datas.get(7) / 100);
                    bandInfo.setBandVersionCode(datas.get(8));

                    int type = datas.get(15);
                    bandInfo.setCanSetStepLength(((type >> 0) & 0x01) == 0);
                    bandInfo.setCanSetSleepTime(((type >> 1) & 0x01) == 0);
                    bandInfo.setCanSet12Hours(((type >> 2) & 0x01) == 0);
                    bandInfo.setHasWeixinSport(((type >> 3) & 0x01) == 0);
                    bandInfo.setHasHeartWarn(((type >> 4) & 0x01) == 1);
                    bandInfo.setNordic(((type >> 5) & 0x01) == 0);
                    bandInfo.setNeedPhoneSerialNumber(((type >> 6) & 0x01) == 1);

                    //长度超过16字节
                    if (datas.size() > 16) {
                        bandInfo.setBandType(datas.get(16));

                        if (bandInfo.getBandType() == 0x0B
                                || bandInfo.getBandType() == 0x0D
                                || bandInfo.getBandType() == 0x0E
                                || bandInfo.getBandType() == 0x0F) {
                            //带连续心率的手环
                            Config.hasContinuousHeart = true;

                        } else if (bandInfo.getBandType() == 0x0c) {
                            //心电手环
                            Config.hasECG = true;
                        } else {
                            //普通手环
                            Config.general = true;
                        }


                        //长度超过17字节
                        if (datas.size() > 17) {
                            int type1 = datas.get(17);
                            bandInfo.setHasPagesManager(((type1 >> 0) & 0x01) == 1);
                            bandInfo.setHasInstagram(((type1 >> 1) & 0x01) == 1);
                            bandInfo.setHasJiuzuotixing(((type1 >> 2) & 0x01) == 1);
                            bandInfo.setHeartRateSaveBattery(((type1 >> 3) & 0x01) == 1);
                            bandInfo.setHeartRateHongwai(((type1 >> 4) & 0x01) == 1);
                            bandInfo.setMoreMessage(((type1 >> 5) & 0x01) == 1);
                        }
                    }


                    object = bandInfo;

                    break;


                case 0x51:
                    //记录数据的时间
                    int year = datas.get(6) + 2000;
                    int month = datas.get(7);
                    int day = datas.get(8);
                    int hour = datas.get(9);
                    int min = datas.get(10);
                    String time = year + String.format("%02d", month)
                            + String.format("%02d", day)
                            + String.format("%02d", hour)
                            + String.format("%02d", min);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
                    long timeInMillis = 0;
                    try {
                        timeInMillis = sdf.parse(time).getTime();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }


                    if (Config.hasContinuousHeart) {
                        //如果是连续心率手环
                        if (datas.get(5) == 0x11) {
                            //单机测量 连续心率数据
                            int hrValue = datas.get(11);
                            HeartRateBean heartRateBean = new HeartRateBean();
                            heartRateBean.setTimeInMillis(timeInMillis);
                            heartRateBean.setHeartRate(hrValue);

                            object = heartRateBean;

                        } else if (datas.get(5) == 0x12) {
                            //单机测量 血氧数据
                            int bloodOxygen = datas.get(11);
                            BloodOxygenBean bloodOxygenBean = new BloodOxygenBean();
                            bloodOxygenBean.setBloodOxygen(bloodOxygen);
                            bloodOxygenBean.setTimeInMillis(timeInMillis);

                            object = bloodOxygen;


                        } else if (datas.get(5) == 0x14) {
                            //单机测量 血压数据
                            int bloodPressure = datas.get(11);
                            BloodPressureBean bloodPressureBean = new BloodPressureBean();
                            bloodPressureBean.setBloodPressure(bloodPressure);
                            bloodPressureBean.setTimeInMillis(timeInMillis);

                            object = bloodPressureBean;


                        } else if (datas.get(5) == 0x20) {
                            //整点数据
                            int steps = (datas.get(10) << 16) + (datas.get(11) << 8) +
                                    datas.get(12);

                            int calory = (datas.get(13) << 16) + (datas.get(14) << 8) +
                                    datas.get(15);

                            int heartRate = datas.get(16);
                            int bloodOxygen = datas.get(17);
                            int bloodPressure_high = datas.get(18);
                            int bloodPressure_low = datas.get(19);


                            HourlyMeasureDataBean hourlyMeasureDataBean = new HourlyMeasureDataBean();
                            hourlyMeasureDataBean.setSteps(steps);
                            hourlyMeasureDataBean.setCalory(calory);
                            hourlyMeasureDataBean.setHeartRate(heartRate);
                            hourlyMeasureDataBean.setBloodOxygen(bloodOxygen);
                            hourlyMeasureDataBean.setBloodPressure_high(bloodPressure_high);
                            hourlyMeasureDataBean.setBloodPressure_low(bloodPressure_low);
                            hourlyMeasureDataBean.setTimeInMillis(timeInMillis+3600*1000);//整点数据时间加一个小时

                            int shallowSleep = datas.get(21) * 60 + datas.get(22);
                            int deepSleep = datas.get(23) * 60 +datas.get(24);
                            int wakeupTimes = datas.get(25);
                            hourlyMeasureDataBean.setShallowSleep(shallowSleep);
                            hourlyMeasureDataBean.setDeepSleep(deepSleep);
                            hourlyMeasureDataBean.setWakeupTimes(wakeupTimes);


                            object = hourlyMeasureDataBean;

                        } else if (datas.get(5) == 0x08) {
                            //当前计步、卡路里、睡眠值

                            int steps = (datas.get(6) << 16) + (datas.get(7) << 8) + datas.get(8);
                            int calory = (datas.get(9) << 16) + (datas.get(10) << 8) + datas.get(11);
                            int shallowSleep = datas.get(12) * 60 + datas.get(13);//分
                            int deepSleep = datas.get(14) * 60 + datas.get(15);
                            int wakeupTimes = datas.get(16);

                            CurrentDataBean currentDataBean = new CurrentDataBean();
                            currentDataBean.setSteps(steps);
                            currentDataBean.setCalory(calory);
                            currentDataBean.setShallowSleep(shallowSleep);
                            currentDataBean.setDeepSleep(deepSleep);
                            currentDataBean.setWakeupTimes(wakeupTimes);
                            currentDataBean.setTimeInMillis(System.currentTimeMillis());

                            object = currentDataBean;


                        }


                    } else if (Config.hasECG) {
                        //如果是心电手环

                    } else {
                        //如果是普通手环
                        if (datas.get(5) == 0x11) {
                            //单机测量 连续心率数据
                            int hrValue = datas.get(11);
                            HeartRateBean heartRateBean = new HeartRateBean();
                            heartRateBean.setTimeInMillis(timeInMillis);
                            heartRateBean.setHeartRate(hrValue);

                            object = heartRateBean;

                        } else if (datas.get(5) == 0x12) {
                            //单机测量 血氧数据
                            int bloodOxygen = datas.get(11);
                            BloodOxygenBean bloodOxygenBean = new BloodOxygenBean();
                            bloodOxygenBean.setBloodOxygen(bloodOxygen);
                            bloodOxygenBean.setTimeInMillis(timeInMillis);

                            object = bloodOxygen;


                        } else if (datas.get(5) == 0x14) {
                            //单机测量 血压数据
                            int bloodPressure = datas.get(11);
                            BloodPressureBean bloodPressureBean = new BloodPressureBean();
                            bloodPressureBean.setBloodPressure(bloodPressure);
                            bloodPressureBean.setTimeInMillis(timeInMillis);

                            object = bloodPressureBean;


                        } else if (datas.get(5) == 0x20) {
                            //整点数据
                            int steps = (datas.get(10) << 16) + (datas.get(11) << 8) +
                                    datas.get(12);

                            int calory = (datas.get(13) << 16) + (datas.get(14) << 8) +
                                    datas.get(15);

                            int heartRate = datas.get(16);
                            int bloodOxygen = datas.get(17);
                            int bloodPressure_high = datas.get(18);
                            int bloodPressure_low = datas.get(19);


                            HourlyMeasureDataBean hourlyMeasureDataBean = new HourlyMeasureDataBean();
                            hourlyMeasureDataBean.setSteps(steps);
                            hourlyMeasureDataBean.setCalory(calory);
                            hourlyMeasureDataBean.setHeartRate(heartRate);
                            hourlyMeasureDataBean.setBloodOxygen(bloodOxygen);
                            hourlyMeasureDataBean.setBloodPressure_high(bloodPressure_high);
                            hourlyMeasureDataBean.setBloodPressure_low(bloodPressure_low);
                            hourlyMeasureDataBean.setTimeInMillis(timeInMillis);


                            int shallowSleep = datas.get(21) * 60 + datas.get(22);
                            int deepSleep = datas.get(23) * 60 +datas.get(24);
                            int wakeupTimes = datas.get(25);
                            hourlyMeasureDataBean.setShallowSleep(shallowSleep);
                            hourlyMeasureDataBean.setDeepSleep(deepSleep);
                            hourlyMeasureDataBean.setWakeupTimes(wakeupTimes);


                            object = hourlyMeasureDataBean;

                        } else if (datas.get(5) == 0x08) {
                            //当前计步、卡路里、睡眠值

                            int steps = (datas.get(6) << 16) + (datas.get(7) << 8) + datas.get(8);
                            int calory = (datas.get(9) << 16) + (datas.get(10) << 8) + datas.get(11);
                            int shallowSleep = datas.get(12) * 60 + datas.get(13);//分
                            int deepSleep = datas.get(14) * 60 + datas.get(15);
                            int wakeupTimes = datas.get(16);

                            CurrentDataBean currentDataBean = new CurrentDataBean();
                            currentDataBean.setSteps(steps);
                            currentDataBean.setCalory(calory);
                            currentDataBean.setShallowSleep(shallowSleep);
                            currentDataBean.setDeepSleep(deepSleep);
                            currentDataBean.setWakeupTimes(wakeupTimes);
                            currentDataBean.setTimeInMillis(System.currentTimeMillis());

                            object = currentDataBean;


                        }

                    }


                    break;
                case 0x52:
                    break;
                default:

                    break;
            }
        }


        return object;
    }


}
