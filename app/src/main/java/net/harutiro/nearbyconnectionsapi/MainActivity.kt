package net.harutiro.nearbyconnectionsapi

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import net.harutiro.nearbyconnectionsapi.ui.theme.NearbyConnectionsAPITheme
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : ComponentActivity() {


    val nickName = "harutiro"
    val SERVICE_ID =  "net.harutiro.nearbyconnectionsapi"

    val activity: MainActivity = this

    var mRemoteEndpointId:String? = ""

    val PERMISSION_REQUEST_CODE = 100

    val TAG = "myapp"

    var mainViewModel:MainViewModel? = null


    val permissions = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE

        )
    }else{
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }


    private fun startAdvertise(){
        Nearby.getConnectionsClient(this)
            .startAdvertising(
                nickName,
                SERVICE_ID,
                mConnectionLifecycleCallback,
                AdvertisingOptions(Strategy.P2P_STAR)
            )
            .addOnSuccessListener {
                Log.d(TAG,"Advertise開始した")
                // Advertise開始した
            }
            .addOnFailureListener {
                Log.d(TAG,"Advertiseできなかった")
                // Advertiseできなかった
            }
    }


    private fun startDiscovery() {
        Nearby.getConnectionsClient(this)
            .startDiscovery(
                SERVICE_ID,
                mEndpointDiscoveryCallback,
                DiscoveryOptions(Strategy.P2P_STAR)
            )
            .addOnSuccessListener {
                Log.d(TAG,"Discovery開始した")
                // Discovery開始した
            }
            .addOnFailureListener {
                Log.d(TAG,"Discovery開始できなかった")
                // Discovery開始できなかった
            }
    }

    private val mEndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            // Advertise側を発見した
            Log.d(TAG,"Advertise側を発見した")


            // とりあえず問答無用でコネクション要求してみる
            Nearby.getConnectionsClient(activity)
                .requestConnection(nickName, endpointId, mConnectionLifecycleCallback)


        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG,"見つけたエンドポイントを見失った")

            // 見つけたエンドポイントを見失った
        }
    }

    private val mConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG,"他の端末からコネクションのリクエストを受け取った時")

            // 他の端末からコネクションのリクエストを受け取った時

            // とりあえず来る者は拒まず即承認
            Nearby.getConnectionsClient(activity)
                .acceptConnection(endpointId, mPayloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d(TAG,"コネクションリクエストの結果を受け取った時")

            // コネクションリクエストの結果を受け取った時

            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG,"コネクションが確立した。今後通信が可能。")

                    // コネクションが確立した。今後通信が可能。
                    // 通信時にはendpointIdが必要になるので、フィールドに保持する。
                    mRemoteEndpointId = endpointId
                }

                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG,"コネクションが拒否された時。通信はできない。")

                    // コネクションが拒否された時。通信はできない。
                    mRemoteEndpointId = null
                }

                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.d(TAG,"エラーでコネクションが確立できない時。通信はできない。")

                    // エラーでコネクションが確立できない時。通信はできない。
                    mRemoteEndpointId = null
                }
            }
        }

        // コネクションが切断された時
        override fun onDisconnected(endpointId: String) {
            Log.d(TAG,"コネクションが切断された時")

            mRemoteEndpointId = null
        }

    }

    private val mPayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    Log.d(TAG,"バイト配列を受け取った時")

                    // バイト配列を受け取った時
                    val data = payload.asBytes()!!
                    mainViewModel?.getData = String(data)
                    Log.d(TAG,data.toString())
                    Log.d(TAG,String(data))


                    // 処理
                }
                Payload.Type.FILE -> {
                    Log.d(TAG,"ファイルを受け取った時")

                    // ファイルを受け取った時
                    val file = payload.asFile()!!
                    Log.d(TAG,file.toString())

                    // 処理
                }
                Payload.Type.STREAM -> {
                    Log.d(TAG,"ストリームを受け取った時")

                    // ストリームを受け取った時
                    val stream = payload.asStream()!!
                    Log.d(TAG,stream.toString())

                    // 処理
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            Log.d(TAG,"転送状態が更新された時詳細は省略")

            // 転送状態が更新された時詳細は省略
        }
    }


    fun datePush(){
        val data = "Hello world".toByteArray()
        val payload = Payload.fromBytes(data)

        Nearby.getConnectionsClient(this)
            .sendPayload(mRemoteEndpointId.toString(), payload)

        Log.d(TAG,"データを送りつけた")
    }

    @Composable
    fun MainScreen(viewModel: MainViewModel){
        var state by remember { mutableStateOf("") }

        NearbyConnectionsAPITheme {
            // A surface container using the 'background' color from the theme
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.background
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, // 横方向
                    verticalArrangement = Arrangement.Center // 縦方向
                ) {
                    Button(
                        onClick = {
                            startDiscovery()
                            state = "データ受信待機"
                        }
                    ){
                        Text("データ受信待機")
                    }

                    Button(
                        onClick = {
                            startAdvertise()
                            state = "データ送信待機"

                        }
                    ){
                        Text("データ送信待機")
                    }

                    Button(
                        onClick = {
                            datePush()
                            state = "データ送信"

                        }
                    ){
                        Text("データ送信")
                    }

                    Text(state)
                    Text(viewModel.getData)

                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!EasyPermissions.hasPermissions(this, *permissions)) {
            // パーミッションが許可されていない時の処理
            EasyPermissions.requestPermissions(activity, "パーミッションに関する説明", PERMISSION_REQUEST_CODE, *permissions)
        }

        setContent {
            mainViewModel = viewModel()
            MainScreen(mainViewModel!!)
        }
    }
}


class MainViewModel: ViewModel() {
    var getData: String by mutableStateOf("")
        internal set
}


@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    NearbyConnectionsAPITheme {
        Greeting("Android")
    }
}