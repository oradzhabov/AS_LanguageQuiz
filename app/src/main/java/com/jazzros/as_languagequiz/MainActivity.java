package com.jazzros.as_languagequiz;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

// Base of wav
// http://www.grsites.com/archive/sounds/category/25/?offset=24

// Tips for publishing
// http://developer.alexanderklimov.ru/android/publish.php

public class MainActivity extends Activity {
    final String TAG = "AnimalQuiz";
    private Random random; // генератор случайных чисел
    private Animation shakeAnimation; // анимация для неверного ответа
    private MediaPlayer mp = null;
    private Handler handler;

    private List<String> animalList;
    private Map<String, List<String>> imgPathByAnimalMap;

    private List<String> gameImgPathList;
    private int         gameCorrectIndex;
    private int         gameGuessNumber;
    private int         gameCorrectAnswerNumber;

    private TableLayout buttonTableLayout;
    private TextView answerTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ///
        handler = new Handler(); // выполнение операций с задержкой
        random = new Random(); // инициализация генератора случайных чисел

        animalList = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.animalList)));
        gameImgPathList = new ArrayList<String>();


        buttonTableLayout = (TableLayout)this.findViewById( R.id.buttonTableLayout);
        answerTextView = (TextView)this.findViewById( R.id.answerTextView);

        imgPathByAnimalMap = new HashMap<String, List<String>>();

        AssetManager assets = getAssets();
        try
        {
            for (String animal : animalList)
            {
                String[] paths = assets.list(animal.toLowerCase());

                for (String path : paths)
                {
                    if (imgPathByAnimalMap.get(animal) == null)
                        imgPathByAnimalMap.put(animal, new ArrayList<String>());
                    imgPathByAnimalMap.get(animal).add(path);
                }
            }
        }
        catch (IOException e)
        {
            Log.e(TAG, "Error loading image file names", e);
            e.printStackTrace();
        }
        shakeAnimation = AnimationUtils.loadAnimation(this, R.animator.incorrect_shake);
        shakeAnimation.setRepeatCount(3); // троекратное повторение анимации

        ImageButton btnPlay = (ImageButton)this.findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(playBtnListener);

        //StartGame();
    }

    @Override
    protected void onStart() {
        super.onStart();

        StartGame();
    }

    @Override
    protected void onPause() {

        StopPlayAudio();

        super.onPause();
    }

    private void StartGame() {

        StartQuiz();

        gameGuessNumber = 0;
        gameCorrectAnswerNumber = 0;
    }

    private void StartQuiz() {
        // Initialize Object
        gameImgPathList.clear();
        answerTextView.setText("");

        for (int row = 0; row < buttonTableLayout.getChildCount(); ++row)
            ((TableRow) buttonTableLayout.getChildAt(row)).removeAllViews();

        StopPlayAudio();
        //Collections.shuffle(animalList);

        TextView questionNumberTextView = (TextView)this.findViewById(R.id.questionNumberTextView);
        questionNumberTextView.setText("Question " + gameCorrectAnswerNumber + " from 10");
        //
        AssetManager assets = getAssets();

        List<Integer> imgAnimalIndices = new ArrayList<Integer>();
        for (int i = 0; i < 4; ++i){
            int randomIndex;
            do {
                randomIndex = random.nextInt(animalList.size());
            } while (imgAnimalIndices.contains(randomIndex) == true);
            imgAnimalIndices.add(randomIndex);

            String randomUniqueAnimal = animalList.get(randomIndex);

            List<String> imgUniqueAnimalPath = imgPathByAnimalMap.get(randomUniqueAnimal);
            int randomIndex2 = random.nextInt(imgUniqueAnimalPath.size());

            final String path = randomUniqueAnimal + "/" + imgUniqueAnimalPath.get(randomIndex2);
            gameImgPathList.add(path);
        }
        ///////////////
        LayoutInflater inflater = (LayoutInflater) getSystemService( Context.LAYOUT_INFLATER_SERVICE);
        final int guessRows = 2;
        for (int row = 0; row < guessRows; row++)
        {
            TableRow currentTableRow = getTableRow(row);

            for (int column = 0; column < 2; column++)
            {
                // «раздувание» guess_button.xml для создания новой кнопки
                ImageButton newGuessButton =
                        (ImageButton) inflater.inflate(R.layout.guess_button, null);

                // получение названия страны и использование в виде текста newGuessButton
                String path_in_assets = gameImgPathList.get((row * 2) + column);
                try {
                    InputStream is = assets.open(path_in_assets.toLowerCase());

                    Bitmap image = BitmapFactory.decodeStream(is);

                    newGuessButton.setImageBitmap(image);
                    String animalName = path_in_assets.substring(0, path_in_assets.lastIndexOf('/'));
                    newGuessButton.setTag(animalName);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // регистрация answerButtonListener для ответа на нажатия кнопок
                newGuessButton.setOnClickListener(guessButtonListener);
                currentTableRow.addView(newGuessButton);
            } // конец цикла for
        } // конец цикла for
        //
        gameCorrectIndex = random.nextInt(4);
        PlayCorrectAudio();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private TableRow getTableRow(int row)
    {
        return (TableRow) buttonTableLayout.getChildAt(row);
    }

    private String CorrectAnimalName() {
        String correctAnimnalImgPath = gameImgPathList.get(gameCorrectIndex);
        return correctAnimnalImgPath.substring(0, correctAnimnalImgPath.lastIndexOf('/'));
    }
    private void RestartGame()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.reset_quiz);

        builder.setMessage(String.format("%d %% correctly answers", 100 - (int)(((double)gameGuessNumber - 10.0)/10.0*100.0)));

        builder.setCancelable(false);

        builder.setPositiveButton(R.string.reset_quiz,
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        StartGame();
                    }
                }
        );

        // создание AlertDialog на основе Builder
        AlertDialog resetDialog = builder.create();
        resetDialog.show(); // отображение диалогового окна

    }
    private void submitGuess(ImageButton guessButton) {

        final String animalName = (String)guessButton.getTag();

        gameGuessNumber++;

        if (animalName.equals(CorrectAnimalName()) == true)
        {
            enableButtons(false);
            answerTextView.setText(animalName + "!");
            answerTextView.setTextColor(
                    getResources().getColor(R.color.correct_answer));

            handler.postDelayed(new Runnable() {
                public void run() {
                    gameCorrectAnswerNumber++;
                    if (gameCorrectAnswerNumber < 10) {
                        StartQuiz();
                    } else {
                        RestartGame();
                    }
                }
            }, 1000); // 1000 миллисекунд (секундная задержка)
        }
        else {
            answerTextView.setText(animalName + "?");
            answerTextView.setTextColor(
                    getResources().getColor(R.color.incorrect_answer));

            guessButton.startAnimation(shakeAnimation);
            guessButton.setEnabled(false);
        }
    }

    private View.OnClickListener guessButtonListener = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            submitGuess((ImageButton) view);
        }
    };
    private View.OnClickListener playBtnListener = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            PlayCorrectAudio();
        }
    };
    private void StopPlayAudio()
    {
        if (mp != null) {
            if (mp.isPlaying()) {
                mp.stop();
                mp.release();
                mp = null;
            }
        }
    }

    private void PlayCorrectAudio()
    {
        final String animalName = CorrectAnimalName();
        final String folderPath = "raw"+ File.separator + "" + "words" + File.separator  + "polski" + File.separator  + animalName;


        AssetManager assets = getAssets();
        String fileName = new String();
        try
        {
            String[] paths = assets.list(folderPath.toLowerCase());
            int index = random.nextInt(paths.length * 100) % paths.length;
            fileName = folderPath + File.separator + paths[index];
        }
        catch (IOException e)
        {
            Log.e(TAG, "Error loading image file names", e);
            e.printStackTrace();
        }

        try {
            StopPlayAudio();

            mp = new MediaPlayer();
            AssetFileDescriptor afd = assets.openFd(fileName.toLowerCase());
            mp.setDataSource(
                    afd.getFileDescriptor(),
                    afd.getStartOffset(),
                    afd.getLength()
            );
            afd.close();

            // Obtain actual audio volume
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            int volume_level= am.getStreamVolume(AudioManager.STREAM_MUSIC);
            am.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    volume_level,
                    0);

            mp.prepare();
            mp.start();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    private void enableButtons (boolean value) {
        for (int row = 0; row < buttonTableLayout.getChildCount(); ++row) {
            TableRow    tr = (TableRow) buttonTableLayout.getChildAt(row);

            final int chCnt = tr.getChildCount();
            for (int i=0; i < chCnt; ++i) {
                tr.getChildAt(i).setEnabled(value);
            }
        }

    }
}
