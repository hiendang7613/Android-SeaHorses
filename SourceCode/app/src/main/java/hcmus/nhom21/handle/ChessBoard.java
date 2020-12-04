package hcmus.nhom21.handle;

import android.database.Cursor;
import android.widget.ImageView;

import java.util.ArrayList;

public class ChessBoard {
    //Các biến liên quan đến widget của activity
    private Tuple LOCATE_BOARD;
    private Tuple SIZE_BOARD;
    private Tuple SIZE_HORSE;
    private Database database;

    //Hằng số
    final int NUM_USER = 4;
    final int NUM_HORSE=4;
    final int TARGET = 56;

    private ArrayList<User> listUser;
    private ArrayList<Tuple> listIdHorse;// Luu ngua voi status =1 dang duoc chay voi x la idUser va y là idHorse

    private int userTurn;
    private int horseTurn;
    private int step;
    private boolean isRepeat;

    public ChessBoard(Tuple LOCATE_BOARD, Tuple SIZE_BOARD, Tuple SIZE_HORSE, Database database){
        this.LOCATE_BOARD=LOCATE_BOARD;
        this.SIZE_BOARD=SIZE_BOARD;
        this.SIZE_HORSE=SIZE_HORSE;
        this.database = database;
    }

    public void initChessBoard(ArrayList<ImageView> imgHorse){
        //int[] horseInitialCoord=new int[2];
        //imgHorse.get(0).getLocationOnScreen(horseInitialCoord);
        //System.out.println("IMGHORSE:   " + horseInitialCoord[0] + "&&&&" +horseInitialCoord[1] + "\n");
        listUser=new ArrayList<User>(NUM_USER);
        listIdHorse = new ArrayList<Tuple>();
        userTurn=0;
        horseTurn=-1;

        //Khởi tạo User và những con ngựa nó quản lý
        for (int idUser = 0; idUser < NUM_USER; idUser++) {
            User user = new User(idUser, LOCATE_BOARD, SIZE_BOARD, SIZE_HORSE);
            listUser.add(idUser, user);

            for (int idHorse = 0; idHorse < NUM_HORSE; idHorse++) {
                user.getListHorse().add(idHorse, new Horse(imgHorse.get(idUser * NUM_HORSE + idHorse), idUser * 14, idUser, idHorse));
                user.setInitialHorseCoord(idHorse);
                //user.setHorseCoordByPosition(idHorse);
            }
        }

    }

    public void loadChessBoard(){
        Cursor dataHorse = database.getData("SELECT * FROM Horse");
        Cursor dataChessBoard = database.getData("SELECT * FROM ChessBoard");

        int idHorse, idUser, position, level, stepped;
        while (dataHorse.moveToNext()) {
            int idLogic = dataHorse.getInt(0);
            idHorse = idLogic % 4;
            idUser = idLogic / 4;
            position = dataHorse.getInt(1);
            level = dataHorse.getInt(2);
            stepped = dataHorse.getInt(3);

            //System.out.println(idHorse+" "+idUser+" "+id+" "+" "+" ");
            listIdHorse.add(new Tuple(idUser, idHorse));
            Horse horse = getHorse(new Tuple(idUser, idHorse));
            horse.setPosition(position);
            horse.setStatus(1);
            horse.setLevel(level);
            horse.setStatus(stepped);
            User user=listUser.get(idUser);
            user.setHorseCoordByPosition(idHorse);
        }

        while (dataChessBoard.moveToNext()) {
            userTurn = dataChessBoard.getInt(0);
            horseTurn = dataChessBoard.getInt(1);
            step = dataChessBoard.getInt(2);
            isRepeat= (dataChessBoard.getInt(3)==1);
        }

        database.queryData("DROP TABLE Horse");
        database.queryData("DROP TABLE User");
    }

    public void saveChessBoard(){
        //Tạo bảng
        database.queryData("CREATE TABLE IF NOT EXISTS Horse(id INTEGER PRIMARY KEY, position INTEGER, level INTEGER, stepped INTEGER)");
        database.queryData("CREATE TABLE IF NOT EXISTS ChessBoard(userTurn INTEGER, horseTURN Integer, step Integer, isRepeat INTEGER)");
        //Insert data horse đang di chuyển
        Horse horse;
        int idLogic;
        for (int i = 0; i < listIdHorse.size(); i++) {
            horse = getHorse(listIdHorse.get(i));
            idLogic = horse.getIdUser() * 4 + horse.getIdHorse();
            database.queryData("INSERT INTO Horse VALUES (" + idLogic + "," + horse.getPosition() + "," + horse.getLevel() + "," + horse.getStepped() + ")");
        }

        //Insert turn sleeped
        database.queryData("INSERT INTO ChessBoard VALUES (" + userTurn + "," + horseTurn + "," + step + "," + (isRepeat?1:0) + ")");

    }

    public Tuple rollDice(){
        User user= listUser.get(userTurn);
        Dice dice = new Dice(1, 1);
        boolean isLucky=false;

        if(isRepeat){
            do {
                step = dice.rollDice();
            }
            while(dice.getNumDice1() == dice.getNumDice2());
        }
        else {
            for (int idHorse = 0; idHorse < NUM_HORSE; idHorse++) {
                if (user.getHorse(idHorse).getStatus() == 1)
                    break;
                if (idHorse == NUM_HORSE - 1)
                    isLucky = true;
            }
            do {
                step = dice.rollDice();
            }
            while (isLucky && dice.getNumDice1() != dice.getNumDice2());
        }
        isRepeat = false;//Mặc định gán false
        if (dice.getNumDice1() == dice.getNumDice2() || (step == 7 && (dice.getNumDice1() == 1 || dice.getNumDice1() == 6)))
            isRepeat = true;
        System.out.println("Buoc nhay: "+ dice.getNumDice1() +"----"+dice.getNumDice2() +"\n");
        return new Tuple(dice.getNumDice1(),dice.getNumDice2());
    }

    public ArrayList<Integer> generateHorseValid() {
        Tuple errorConflict = new Tuple(0,0);
        Horse horse = null;
        User user = listUser.get(userTurn);
        //System.out.println("Buoc nhay: "+ dice.getNumDice1() +"----"+dice.getNumDice2() +"\n");

        //Khởi tạo mảng ngụa có thể chạy
        ArrayList<Integer> horseValid = new ArrayList<Integer>();
        for (int idHorse = 0; idHorse < NUM_HORSE; idHorse++) {
            horse = user.getHorse(idHorse);
            errorConflict = checkConflict(horse, step);

            Tuple idPair = new Tuple();
            try {
                idPair = listIdHorse.get(errorConflict.x);
            }catch (Exception e){};

            int conflictCode=errorConflict.y;
            if (horse.getStatus() == 1 && conflictCode != -1 && idPair.x != userTurn) {
                horseValid.add(idHorse);
            }
            else if (isRepeat && horse.getStatus() == 0) {
                errorConflict = checkConflict(horse, 0);
                try {
                    idPair = listIdHorse.get(errorConflict.x);
                }catch (Exception e){};
                if(errorConflict.y != -1 && idPair.x != userTurn)
                    horseValid.add(idHorse);
            }
        }
        if(horseValid.size()<=0) {
            userTurn= (userTurn+1)%4;
        }
        //System.out.println("HORSE VALID: "+horseValid.size()+"\n");
        return horseValid;
    }
    public void moveHorse(int step){
        User user=listUser.get(userTurn);
        Horse horse=user.getHorse(horseTurn);
        user.MoveHorse(horseTurn,step);
        //System.out.println("DI CHUYEN HORSE: " +horse.getPosition()+"---("+horse.getCoord().x + "," +horse.getCoord().y+") \n");
    }
    public void updateChessBoard(){
        User user=listUser.get(userTurn);
        user.setImgHorse(horseTurn);
    }

    public Tuple checkConflict(Horse horse, int step) { //Trả về 1 cặp biến lỗi vs biến đầu là vị trí ngựa trong listIdHorse và biến sau là mã lỗi
        Tuple errorConflict=new Tuple(-1,0);
        for (int i = 0; i < listIdHorse.size(); i++) {

            Horse otherHorse = getHorse(listIdHorse.get(i));
            int newPosition = (horse.getPosition() + step)%TARGET;
            int newRound = (horse.getPosition() + step)/TARGET;
            if ( newPosition == otherHorse.getPosition()) {
                //System.out.println("CONFLICT  1 "+ idPair.x + "|||||" +idPair.y +"\n");
                errorConflict = new Tuple(i,1);
            }
            if ((newRound>=1 || horse.getPosition() < otherHorse.getPosition() ) && newPosition > otherHorse.getPosition()) {
                errorConflict = new Tuple(i,-1);
                break;
            }

        }
        return errorConflict;
    }

    public void Dangua(int id) {
        //System.out.println("CONFLICT "+ idPair.x + "|||||" +idPair.y +"\n");
        Tuple idPair = listIdHorse.get(id);
        User user = listUser.get(idPair.x);
        user.setInitialHorseCoord(idPair.y);
        listIdHorse.remove(id);
        //listHorse.remove(horse.getPosition() + 1);
    }

    public void XuatChuong() {
        User user = listUser.get(userTurn);

        Tuple errorConflict = new Tuple(-1,0);

        Horse horse = user.getHorse(horseTurn);
        errorConflict = checkConflict(horse, 0);
        int conflictCode = errorConflict.y;
        Tuple idPair =new Tuple();
        if (conflictCode != 0) {
            idPair=listIdHorse.get(errorConflict.x);
            if(idPair.x != userTurn)
                Dangua(errorConflict.x);
        }
        listIdHorse.add(new Tuple(userTurn,horseTurn));
        user.setHorseCoordByPosition(horseTurn);
        System.out.println("Xuat chuong thanh cong "+ user.getHorse(horseTurn).getStatus());
        //System.out.println("CONFLICT: "+ listIdHorse.size() +"\n");
    }

    public Horse getHorse(Tuple idPair) {
        User user = listUser.get(idPair.x);
        Horse horse = user.getHorse(idPair.y);
        return horse;
    }

    public boolean isRepeat() {
        return isRepeat;
    }

    public void setRepeat(boolean repeat) {
        isRepeat = repeat;
    }

    public User getUser(){
        return listUser.get(userTurn);
    }

    public User getUser(int idUser){
        return listUser.get(idUser);
    }

    public ArrayList<User> getListUser() {
        return listUser;
    }

    public void setListUser(ArrayList<User> listUser) {
        this.listUser = listUser;
    }

    public ArrayList<Tuple> getListIdHorse() {
        return listIdHorse;
    }

    public void setListIdHorse(ArrayList<Tuple> listIdHorse) {
        this.listIdHorse = listIdHorse;
    }

    public int getUserTurn() {
        return userTurn;
    }

    public void setUserTurn(int userTurn) {
        this.userTurn = userTurn;
    }

    public int getHorseTurn() {
        return horseTurn;
    }

    public void setHorseTurn(int horseTurn) {
        this.horseTurn = horseTurn;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }
}