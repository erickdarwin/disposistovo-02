package pe.edu.erickdqs.controlbt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class UserInterfaz extends AppCompatActivity {




    Button IdEncender, IdApagar,IdDesconectar;
    TextView IdBufferIn;


    Handler bluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConexionBT;

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String address = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_interfaz);

        IdEncender = (Button)findViewById(R.id.IdEncender);
        IdApagar = (Button)findViewById(R.id.IdApagar);
        //IdDesconectar = (Button)findViewById(R.id.IdDesconectar);
        //IdBufferIn = (TextView) findViewById(R.id.IdbufferIn);

        bluetoothIn = new Handler(){
            public void handleMessage(android.os.Message msg){
                String readMessage = (String) msg.obj;
                DataStringIN.append(readMessage);

                int endOfLineInfex = DataStringIN.indexOf("#");

                if (endOfLineInfex > 0 ){
                    String dataInPrint = DataStringIN.substring(0,endOfLineInfex);
                    IdBufferIn.setText("Dato: " + dataInPrint);
                    DataStringIN.delete(0, DataStringIN.length());
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        VerificarEstadoBT();

        IdEncender.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v)
            {
                MyConexionBT.write ("1");

            }
        });

        IdApagar.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                MyConexionBT.write("0");

            }
        });

        /*IdDesconectar.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if (btSocket!=null)
                {
                    try {btSocket.close();}
                        catch (IOException e )
                        {
                            Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_SHORT).show();}
                            
                }
                finish();
            }
        });*/



    }


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException
    {

        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Intent intent =getIntent();
        address = intent.getStringExtra(DispositivosBT.EXTRA_DEVICE_ADDRESS);

        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try
        {
            btSocket =createBluetoothSocket(device);
        }catch (IOException e){
            Toast.makeText(getBaseContext(), "La creacion del socket fallo", Toast.LENGTH_LONG).show();

        }

        try
        {
            btSocket.connect();

        }catch (IOException e){
            try {
                btSocket.close();
            }catch (IOException e2){}
        }
        MyConexionBT = new  ConnectedThread(btSocket);
        MyConexionBT.start();



    }
    @Override
    public void onPause(){
        super.onPause();
        try
        {

            btSocket.close();
        }catch (IOException e2){}
    }


    private  void VerificarEstadoBT(){

        if (btAdapter == null){

            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        }else {
            if (btAdapter.isEnabled()){

            }else {
                Intent enableBtIntent =new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }


    private class ConnectedThread extends Thread
    {

        private  final InputStream mmInStream;
        private  final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }catch (IOException e){}

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public  void run()
        {
            byte[] buffer =new byte[256];
            int bytes;

            while (true){
                try{
                    bytes = mmInStream.read(buffer);
                    String readMessage =new String(buffer,0,bytes);

                    bluetoothIn.obtainMessage(handlerState, bytes, -1,readMessage).sendToTarget();
                }catch (IOException e){
                    break;
                }
            }
        }

        public  void  write (String input)
        {
            try {
                mmOutStream.write(input.getBytes());
            }catch (IOException e)
            {

                Toast.makeText(getBaseContext(), "La conexion fallo",Toast.LENGTH_LONG);
                finish();
            }
        }
    }

}
