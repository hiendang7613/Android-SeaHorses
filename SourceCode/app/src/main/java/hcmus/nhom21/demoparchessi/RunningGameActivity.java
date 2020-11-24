package hcmus.nhom21.demoparchessi;


import hcmus.nhom21.handle.Database;
import hcmus.nhom21.handle.Dice;
import hcmus.nhom21.handle.Horse;
import hcmus.nhom21.handle.Tuple;
import hcmus.nhom21.handle.User;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;

public class RunningGameActivity extends FragmentActivity{
    private int[] LOCATE_BOARD = new int[2];
    private int[] SIZE_BOARD = new int[2];
    private int[] SIZE_HORSE = new int[2];
    final int NUM_USER = 4;
    final int NUM_HORSE = 4;
    private ArrayList<User> listUser;
    private ArrayList<Tuple> listIdHorse;// Luu ngua voi status =1 dang duoc chay voi x la idUser va y là idHorse
    Database database;

    private Button btnTypePlayer;
    private ImageButton btnSetting;
    private ImageView imgBoard;
    private ImageView imgDice;
    private ArrayList<ImageView> imgHorse;
    boolean flagTypePlayer = false;
    boolean flagHide;
    SettingFragment settingFragment;

    FragmentManager fm;
    FragmentTransaction ft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_runninggame);
        Intent intentMain = getIntent();

        initView();

        //Tạo database trò chơi
        database=new Database(this,"parchessi.sqlite",null,1);

        //Toast.makeText(this, imgBoard.getLocationOnScreen().toString(), Toast.LENGTH_SHORT).show();
        //Bật tắt chế độ auto
        btnTypePlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!flagTypePlayer) {
                    flagTypePlayer = true;
                    btnTypePlayer.setBackgroundResource(R.drawable.ic_baseline_android_24);
                    Toast.makeText(RunningGameActivity.this, "Bật tự động chơi", Toast.LENGTH_SHORT).show();
                } else {
                    flagTypePlayer = false;
                    btnTypePlayer.setBackgroundResource(R.drawable.ic_baseline_person_24);
                    Toast.makeText(RunningGameActivity.this, "Tắt tự động chơi", Toast.LENGTH_SHORT).show();
                }
            }
        });


        //Mở menu cài đặt
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ft = getSupportFragmentManager().beginTransaction();
                btnSetting.setVisibility(View.INVISIBLE);

                SettingFragment settingFragment = new SettingFragment();
                ft.replace(R.id.frameSetting, settingFragment);
                ft.addToBackStack(null);
                ft.commit();

                //Ẩn/Vô hiệu hóa/Chèn fragment lên trên cùng của activiy hiện tại
                findViewById(R.id.imgBoard).setVisibility(View.INVISIBLE);
                findViewById(R.id.txtProfile).setVisibility(View.INVISIBLE);
                flagHide = true;
            }
        });
    }




    @Override
    protected void onPause() {
        saveGame();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        listUser = new ArrayList<User>(4);
        listIdHorse = new ArrayList<Tuple>();


        //Lấy tọa độ bàn cờ

        imgBoard.getLocationOnScreen(LOCATE_BOARD);

        //System.out.println("Tọa độ:   " + LOCATE_BOARD[0] + "&&&&" +LOCATE_BOARD[1] + "/n");

        //Lấy kích thước bàn cờ
        SIZE_BOARD[0] = imgBoard.getDrawable().getIntrinsicWidth();
        SIZE_BOARD[1] = imgBoard.getDrawable().getIntrinsicHeight();
        //System.out.println("SIZE BOARD:   " + SIZE_BOARD[0] + "&&&&" +SIZE_BOARD[1] + "/n");

        //Lấy kích thước ngựa
        SIZE_HORSE[0] = imgHorse.get(0).getMeasuredWidth();
        SIZE_HORSE[1] = imgHorse.get(0).getMeasuredHeight();
        //System.out.println("SIZEHORSE:   " + SIZE_HORSE[0] + "&&&&" +SIZE_HORSE[1] + "\n");

        initGame();
        try {
            loadGame();//Lựa chọn  load game nhận intent từ activity trước
            Toast.makeText(this, "Load game", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e){
            Toast.makeText(this, "init game", Toast.LENGTH_SHORT).show();
        };
    }

    public void initGame(){
        //int[] horseInitialCoord=new int[2];
        //imgHorse.get(0).getLocationOnScreen(horseInitialCoord);
        //System.out.println("IMGHORSE:   " + horseInitialCoord[0] + "&&&&" +horseInitialCoord[1] + "\n");
        //Khởi tạo User và những con ngựa nó quản lý
        for (int idUser = 0; idUser < NUM_USER; idUser++) {
            User user = new User(idUser, new Tuple(LOCATE_BOARD[0], LOCATE_BOARD[1]),
                    new Tuple(SIZE_BOARD[0], SIZE_BOARD[1]), new Tuple(SIZE_HORSE[0], SIZE_HORSE[1]));
            listUser.add(idUser, user);

            for (int idHorse = 0; idHorse < NUM_HORSE; idHorse++) {
                user.getListHorse().add(idHorse, new Horse(imgHorse.get(idUser * NUM_HORSE + idHorse), idUser * 14, idUser, idHorse));
                user.setInitialHorseCoord(idHorse);
                //user.setHorseCoord(idHorse);
            }
        }
        listUser.get(0).setFlag(1);
    }

    public void loadGame(){
        Cursor dataHorse=database.getData("SELECT * FROM Horse");
        Cursor dataUser=database.getData("SELECT * FROM User");

        int idHorse,idUser,position,level,stepped;
        while(dataHorse.moveToNext()){
            int idLogic=dataHorse.getInt(0);
            idHorse=idLogic%4;
            idUser=idLogic/4;
            position=dataHorse.getInt(1);
            level=dataHorse.getInt(2);
            stepped=dataHorse.getInt(3);

            //System.out.println(idHorse+" "+idUser+" "+id+" "+" "+" ");
            listIdHorse.add(new Tuple(idUser,idHorse));
            Horse horse = getHorse(new Tuple(idUser,idHorse));
            horse.setPosition(position);
            horse.setStatus(1);
            horse.setLevel(level);
            horse.setStatus(stepped);
        }

        while(dataUser.moveToNext()){
            idUser=dataUser.getInt(0);
            int step = dataUser.getInt(1);
            User user = listUser.get(idUser);
            user.setStep(step);
        }

        database.queryData("DROP TABLE Horse");
        database.queryData("DROP TABLE User");
    }

    public void saveGame(){
        //Tạo bảng
        database.queryData("CREATE TABLE IF NOT EXISTS Horse(id INTEGER PRIMARY KEY, position INTEGER, level INTEGER, stepped INTEGER)");
        database.queryData("CREATE TABLE IF NOT EXISTS User(id INTEGER PRIMARY KEY, step INTEGER)");
        //Insert data horse đang di chuyển
        Horse horse;
        int idLogic;
        for(int i=0;i<listIdHorse.size();i++){
            horse=getHorse(listIdHorse.get(i));
            idLogic=horse.getIdUser()*4+horse.getIdHorse();
            database.queryData("INSERT INTO Horse VALUES ("+idLogic+","+horse.getPosition()+","+horse.getLevel()+","+horse.getStepped()+")");
        }

        //Insert data user
        User user;
        for(int idUser=0;idUser<listUser.size();idUser++){
            user=listUser.get(idUser);
            if(user.getFlag()==1){
                database.queryData("INSERT INTO User VALUES ("+idUser+","+user.getStep()+")");
                break;
            }
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            //Toast.makeText(RunningGameActivity.this, "Kill running", Toast.LENGTH_SHORT).show();
            if (flagHide) {
                //flagHide = false;
                findViewById(R.id.imgBoard).setVisibility(View.VISIBLE);
                findViewById(R.id.txtProfile).setVisibility(View.VISIBLE);
                findViewById(R.id.btnSetting).setVisibility(View.VISIBLE);
            }
            //finish();
        }
        return super.onKeyDown(keyCode, event);
    }


    public void initView() {
        btnTypePlayer = (Button) findViewById(R.id.btnTypePlayer);
        btnSetting = (ImageButton) findViewById(R.id.btnSetting);
        imgBoard = (ImageView) findViewById(R.id.imgBoard);
        imgDice =(ImageView) findViewById(R.id.imgDice);
        imgHorse = new ArrayList<ImageView>(16);

        imgHorse.add((ImageView) findViewById(R.id.imgHorse00));
        imgHorse.add((ImageView) findViewById(R.id.imgHorse01));
        imgHorse.add((ImageView) findViewById(R.id.imgHorse02));
        imgHorse.add((ImageView) findViewById(R.id.imgHorse03));
        imgHorse.add((ImageView) findViewById(R.id.imgHorse10));
        imgHorse.add((ImageView) findViewById(R.id.imgHorse11));
        imgHorse.add((ImageView) findViewById(R.id.imgHorse12));
        imgHorse.add((ImageView) findViewById(R.id.imgHorse13));
        imgHorse.add((ImageView) findViewById(R.id.imgHorse20));
        imgHorse.add((ImageView) findViewById(R.id.imgHorse21));
        imgHorse.add((ImageView) findViewById(R.id.imgHorse22));
        imgHorse.add((ImageView) findViewById(R.id.imgHorse23));
        imgHorse.add((ImageView) findViewById(R.id.imgHorse30));
        imgHorse.add((ImageView) findViewById(R.id.imgHorse31));
        imgHorse.add((ImageView) findViewById(R.id.imgHorse32));
        imgHorse.add((ImageView) findViewById(R.id.imgHorse33));

    }

    public void Turn() {
        Thread thread= new Thread(new Runnable() {
            @Override
            public void run() {
                for(int idUser=0;idUser<NUM_USER;idUser++) {
                    Tuple idPair = new Tuple();
                    int flagConflict = 0;
                    Dice dice = new Dice(1, 1);
                    Horse horse = null;
                    int step = dice.rollDice();

                    User user = listUser.get(idUser);
                    boolean isRepeat = false;

                    if (dice.getNumDice1() == dice.getNumDice2() || (step == 7 && (dice.getNumDice1() == 1 || dice.getNumDice1() == 6)))
                        isRepeat = true;

                    //Khởi tạo mảng ngụa có thể chạy
                    ArrayList<Horse> horseValid = new ArrayList<>();
                    for (int i = 0; i < NUM_HORSE; i++) {
                        horse = user.getHorse(i);
                        flagConflict = checkConflict(idPair, horse, step);
                        if (horse.getStatus() == 1 && flagConflict != -1 && idPair.x != idUser) {
                            horseValid.add(horse);
                        }
                        if (isRepeat && horse.getStatus() == 0) {
                            horseValid.add(horse);
                        }
                    }

                    boolean isSelected = false;
                    int idHorse = 0;//Ngựa được chọn để chạy
                    while (!isSelected) {
                        for (int i = 0; i < horseValid.size(); i++) {
                            horse = horseValid.get(i);
                            //Đợi lắng nghe để chọn ngựa chạy
                            imgHorse.get(horse.getIdUser() * 4 + horse.getIdHorse()).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                }
                            });
                        }
                        isSelected = false;
                    }

                    if (horse.getStatus() == 0) {
                        XuatChuong(idUser, idHorse);
                        listIdHorse.add(new Tuple(idUser, horse.getIdHorse()));

                    } else {
                        //Xu ly nguoi dung chon ngua trong mang ngua vua nay

                        user.MoveHorse(idHorse, step - 1);

                        if (flagConflict == 1) {
                            Dangua(idPair);
                        }
                        user.MoveHorse(idHorse, 1);
                    }

                    //return isRepeat;
                }
            }
        });
        thread.start();

    }

    public int checkConflict(Tuple idPair, Horse horse, int step) {
        for (int i = 0; i < listIdHorse.size(); i++) {
            Horse otherHorse = getHorse(listIdHorse.get(i));
            if (horse.getPosition() + step == otherHorse.getPosition()) {
                idPair = listIdHorse.get(i);
                return 1;
            }
            if (horse.getPosition() < otherHorse.getPosition() && horse.getPosition() + step > otherHorse.getPosition()) {
                idPair = listIdHorse.get(i);
                return -1;
            }
        }
        return 0;
    }

    public void Dangua(Tuple idPair) {
        Horse horse = getHorse(idPair);

        horse.resetInitial();
        for (int i = 0; i < listIdHorse.size(); i++) {
            if (listIdHorse.get(i).x == idPair.x && listIdHorse.get(i).y == idPair.y) {
                listIdHorse.remove(i);
                break;
            }
        }
        //listHorse.remove(horse.getPosition() + 1);
    }

    public void XuatChuong(int idUser,int idHorse) {
        User user = listUser.get(idUser);
        Tuple idPair = new Tuple();
        int flag = 0;

        Horse horse = user.getHorse(idHorse);
        flag = checkConflict(idPair, horse, 0);

        if (flag != 0 && idPair.x != idUser) {
            Dangua(idPair);
        }
        user.setHorseCoord(idHorse);
    }

    public Horse getHorse(Tuple idPair) {
        User user = listUser.get(idPair.x);
        Horse horse = user.getHorse(idPair.y);
        return horse;
    }


}
