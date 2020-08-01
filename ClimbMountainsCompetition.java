/**
 * FileName: ClimbMountainsCompetition
 * Date:     2020/7/10 12:22 下午
 * Description: 爬山
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */

import java.util.*;
import java.util.concurrent.*;

/**
 * 〈爬山比赛〉
 *  比赛规则: //本次登上过程中的关卡，必须所有成员都到了才可以继续往下一个关卡进发。
 *         //但是在倒数第二个关卡地点的时候只等待2秒钟，然后已经到了的不用等了，开始急需攀登，追逐第一名
 *         //最后角逐第一名
 *         参与类：
 *         CountDownLatch作用：用于控制鸣哨和活动结束（闭锁）
 *         Semaphore作用：用于控制奖牌的获取，只有3个，只允许三个（信号量）
 *         CyclicBarrier作用：关卡裁判员，用于控制参赛选手必须到达某个条件才能继续往后进发。（栅栏）
 * @create 2020/7/10
 * @since 1.0.0
 */
public class ClimbMountainsCompetition {

        //峨眉山关卡
        private final static String[] CHECK_POINTS = new String[]{"报国寺","伏虎寺","清音阁","万年寺","洗象池","雷洞坪","金顶"};
        //参赛人数
        private final static int ACTIVE_NUM = 10;
        //起爬线
        private final static CountDownLatch STARTING_LINE = new CountDownLatch(1);
        //比赛结束状态限制
        private final static CountDownLatch endStatus = new CountDownLatch(1);
        //奖牌池
        private final static BlockingQueue<String> MEDAL_POOL = new ArrayBlockingQueue<>(3);
        //参赛人数
        private final static List<PersonInfo> PERSON_INFOS = new ArrayList<>(ACTIVE_NUM);
        private final static float[] SPEEDS = new float[]{2.0f,2.1f,2.4f,2.7f,2.3f,2.5f,2.4f,2.1f,2.5f,2.1f,2.0f};//各个运动员速率
        public final static String CHAMPION = "冠军奖牌";
        public final static String SECOND_PLACE = "亚军奖牌";
        public final static String THIRD_WINNER_IN_CONTEST = "季军奖牌";

        static{
            System.out.println("-----欢迎来到第20届爬山杯峨眉山站------");
            sleep(1000);
            System.out.println("-----请看主办方正在准备奖牌了----");
            sleep(2000);
            MEDAL_POOL.add(CHAMPION);
            MEDAL_POOL.add(SECOND_PLACE);
            MEDAL_POOL.add(THIRD_WINNER_IN_CONTEST);
            System.out.println("-----奖牌已经准备好,展示-----包括：");
            MEDAL_POOL.forEach(System.out::println);
            int i = ACTIVE_NUM;
            while(i-->0){
                //初始化选手
                PERSON_INFOS.add(new PersonInfo("参赛选手"+i,SPEEDS[i>SPEEDS.length ? SPEEDS.length-1 : i]));
            }
        }


        //奖牌池，冠军，亚军，季军才允许获取
        final Semaphore medalPoolPermit= new Semaphore(3);
        /**关卡裁判员，用于管理先到的人必须等到所有人到来才能进行爬山运动*/
        final CyclicBarrier checkPointReferee = new CyclicBarrier(ACTIVE_NUM, new Champion());
        /**获奖用户集合*/
        final static List<WinningPrizePersonInfo> winningPrizePersonInfos = new ArrayList<>();

        public static void main(String[] args) throws InterruptedException {
            System.out.println("-----参赛选手陆续入场准备-----");
            Thread.sleep(2000);
            new ClimbMountainsCompetition().actionStart();
            System.out.println("------获奖名单揭晓--------");
            Collections.sort(winningPrizePersonInfos);
            System.out.println(winningPrizePersonInfos.toString());
        }

        /**
         * 活动开始
         */
        public void actionStart(){
            for(PersonInfo personInfo :  PERSON_INFOS){
                new PersonClimbMoun(personInfo).start();
            }
            sleep(4000);
            System.out.println("-----比赛倒计时-----");
            System.out.println("-----3-----");
            sleep(1000);
            System.out.println("-----2-----");
            sleep(1000);
            System.out.println("-----1-----");
            sleep(1000);
            System.out.println("鸣枪！比赛开始");
            STARTING_LINE.countDown();//起爬线准备
            try {
                endStatus.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("登山活动结束");
        }

        private static void sleep(int i2) {
            try {
                Thread.sleep(i2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        private class PersonClimbMoun extends Thread{

            private final PersonInfo personInfo;
            public PersonClimbMoun(PersonInfo personInfo){
                this.personInfo = personInfo;
            }
            @Override
            public void run() {
                try {
                    Thread.sleep(1000+new Random().nextInt(1000));
                    System.out.println("选手:"+personInfo.getName()+"已到达起爬线准备");
                    STARTING_LINE.await();//进入起爬线
                    System.out.println("选手:"+personInfo.getName()+"出发了");
                    //当前关卡
                    int currentIdx = 0;
                    for(String expedition : CHECK_POINTS){
                        currentIdx++;
                        Thread.sleep((long)(personInfo.getSpeed() * 3000)+new Random().nextInt(3000));//增加随机数，让结果难以预料
                        try {
                            if(currentIdx == CHECK_POINTS.length -1){//如果到了倒数第二个节点,只等2秒钟,其他没有到达的则视为淘汰
                                System.out.println(personInfo.getName()+"到达"+expedition);
                                checkPointReferee.await(2,TimeUnit.SECONDS);
                            }else if(currentIdx == CHECK_POINTS.length){//终点
                                if(medalPoolPermit.tryAcquire()){
                                    System.out.println(personInfo.getName()+"到达"+expedition);
                                    String medal = MEDAL_POOL.remove();
                                    WinningPrizePersonInfo winningPrizePersonInfo = new WinningPrizePersonInfo();
                                    winningPrizePersonInfo.setName(personInfo.getName());
                                    winningPrizePersonInfo.setMedal(medal);
                                    winningPrizePersonInfos.add(winningPrizePersonInfo);
                                    if(medalPoolPermit.availablePermits() == 0){
                                        endStatus.countDown();
                                    }
                                }else{
                                    return;
                                }
                            }else{
                                System.out.println(personInfo.getName()+"到达"+expedition);
                                checkPointReferee.await();
                            }
                        } catch (BrokenBarrierException e) {
                            System.out.println(personInfo.getName()+"在规定时间未达到目的地,自动被淘汰1111");
                            //e.printStackTrace();
                        } catch (TimeoutException e) {
                            System.out.println(personInfo.getName()+"在规定时间未达到目的地,自动被淘汰");
                            //e.printStackTrace();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    /**
     * 关卡到达后要做的事
     * @author Administrator
     *
     */
    class Champion implements Runnable{
        @Override
        public void run() {
            System.out.println("-------这里是到达关卡后的休息点，想干点啥快点干-------");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //得奖用户信息
    class WinningPrizePersonInfo implements Comparable<WinningPrizePersonInfo>{
        //选手名称
        private String name;

        //奖牌
        private String medal;

        //金牌排前面
        @Override
        public int compareTo(WinningPrizePersonInfo o) {
            if(this.medal.equals(ClimbMountainsCompetition.CHAMPION)){
                return -1;
            }else if(this.medal.equals(ClimbMountainsCompetition.SECOND_PLACE)){
                return 0;
            }else{
                return 1;
            }
        }

        public String getMedal() {
            return medal;
        }

        public void setMedal(String medal) {
            this.medal = medal;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString(){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return  "获得"+getMedal()+"的参赛选手是"+getName()+"\n-------欢呼声------\n";
        }
    }

    class PersonInfo{

        private String name;

        /**
         * 速率,越小代表爬山爬的越快
         */
        private float speed;
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public float getSpeed() {
            return speed;
        }
        public void setSpeed(float speed) {
            this.speed = speed;
        }
        public PersonInfo(String name, float speed) {
            super();
            this.name = name;
            this.speed = speed;
        }
}