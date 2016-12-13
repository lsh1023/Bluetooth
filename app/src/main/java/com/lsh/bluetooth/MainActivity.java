package com.lsh.bluetooth;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    //蓝牙适配器,是操作蓝牙功能的核心类.
    //Represents the local device Bluetooth adapter.
    //蓝牙适配器代表了本地的一个蓝牙设备.
    //The BluetoothAdapter lets you perform fundamental Bluetooth tasks,
    //这个蓝牙适配器能够执行一些列蓝牙任务.
    // such as initiate device discovery, query a list of bonded (paired) devices,
    //比如初始化蓝牙扫描任务,查询之前已经绑定过的蓝牙设备列别;
    // instantiate a BluetoothDevice using a known MAC address,
    //利用已知的MAC地址来初始化一个蓝牙设备.
    // and create a BluetoothServerSocket to listen for connection requests
    // 创建一个用来供其他设备来连接的蓝牙服务端的socket.
    // from other devices, and start a scan for Bluetooth LE devices.
    //也可以扫描一个低功耗的蓝牙设备.LE(low exxx)低功耗.

    private ListView mLv;
    private CheckBox cbFind;
    private TextView tvInfo;

    /**
     * 代表本设备的蓝牙适配器.该适配器中包含了对蓝牙的各种基本操作. 比如设备的发现,配对设备列表查询,利用已知的MAC地址初始化蓝牙设备,
     * 以及创建一个BluetoothServerSocket来监听与其他设备的连接请求, 或者开始扫描蓝牙LE设备.
     * 一旦有了本地的蓝牙适配器对象,就可以通过getBondedDevices()方法来得到代表所有已配对设备的集合.
     */
    private BluetoothAdapter mBluetoothAdapter;

    /**
     * ListView的适配器
     */
    private ArrayAdapter<String> mAdapter;

    /**
     * 已绑定的蓝牙设备
     */
    private Set<BluetoothDevice> bondDevicesSet;

    private ArrayAdapter<String> dialogAdapter;

    /**
     * 存放扫描到的新的蓝牙设备
     */
    private Set<BluetoothDevice> newDeviceSet;

    /**
     * 发现设备的广播接收器
     */
    private FoundDeviceReceiver receiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        newDeviceSet = new HashSet<BluetoothDevice>();

        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mLv.setAdapter(mAdapter);

        CheckBlueToothIsEnable();

        IntentFilter filter = new IntentFilter();
        //过滤已发现的蓝牙设备
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        receiver = new FoundDeviceReceiver();
        registerReceiver(receiver, filter);

    }

    //检查蓝牙是否开启
    private void CheckBlueToothIsEnable() {
        //得到默认的蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            tvInfo.setText("该设备中没有蓝牙功能");
        } else {
            tvInfo.setText("该设备中蓝牙功能");
            //如果蓝牙功能没有打开，则弹出对话框，提示打开蓝牙
            if (!mBluetoothAdapter.isEnabled()) {

                new AlertDialog.Builder(this).setTitle("提示")
                        .setMessage("蓝牙设备未打开，是否现在打开？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                //通过意图，开启蓝牙功能
                                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                startActivityForResult(intent, 1);
                            }
                        }).setCancelable(false).show();
            } else {
                tvInfo.setText("蓝牙设备已经开启");
                getBoundBlueToothDevice();
            }
        }

    }


    //初始化控件
    private void initView() {

        mLv = (ListView) findViewById(R.id.lv_devices);
        cbFind = (CheckBox) findViewById(R.id.cd_enable);
        tvInfo = (TextView) findViewById(R.id.tv_info);
        cbFind.setOnCheckedChangeListener(this);
    }

    //搜索控件
    public void searchDevice(View view) {
        dialogAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        new AlertDialog.Builder(this).setTitle("设备搜索中...").setAdapter(dialogAdapter, new DialogInterface.OnClickListener() {
            // 进行配对处理
            @Override
            public void onClick(DialogInterface dialog, int which) {
                BluetoothDevice device = (BluetoothDevice) newDeviceSet.toArray()[which];

                //如果设备未配对，则配对
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    //先取消一个正在进行中的扫描任务
                    mBluetoothAdapter.cancelDiscovery();

                    try {
                        // 通过反射找到createBond()方法
                        Class<? extends BluetoothDevice> clazz = device.getClass();
                        Method method = clazz.getDeclaredMethod("createBond");
                        Boolean flag = (Boolean) method.invoke(device);

                        if (flag) {
                            Toast.makeText(MainActivity.this, "配对成功", Toast.LENGTH_SHORT).show();
                        }

                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    //如果配对已存在，则取消扫描就可以了
                    mBluetoothAdapter.cancelDiscovery();
                }

            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            // 取消扫描蓝牙设备
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // 取消当前的蓝牙扫描任务.因为对于蓝牙适配器来说,
                // 扫描任务是很繁重的,所以我们每次在通过BluetoothSocket.connect()方法
                // 进行连接远程设备之前都应该调用cancelDiscovery()方法.
                // 扫描任务并不是由Activity来管理,但是它是作为一个系统服务来运行的.
                // 所以一个应用程序应该经常取消扫描任务,即使该任务不是由它自己直接发起请求的.
                mBluetoothAdapter.cancelDiscovery();
            }
        }).setCancelable(false).show();

        //开始扫描
        mBluetoothAdapter.startDiscovery();

    }

    //处理回调结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Toast.makeText(MainActivity.this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
            getBoundBlueToothDevice();
        }

    }

    //获取绑定蓝牙设备
    @SuppressLint("NewApi")
    private void getBoundBlueToothDevice() {
        //返回绑定到本地适配器的蓝牙设备集合
        bondDevicesSet = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bondDevicesSet) {
            mAdapter.add("设备名称:" + device.getName() + ",设备地址:" +
                    device.getAddress() + ",类型:" + device.getType());
        }

    }

    //checkBox勾选事件：开启，允许被别的设备发现
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (isChecked) {
            //开启当前蓝牙设备可以被其他设备检索
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            //取值范围：0~3600，低于或超出，则自动设为120秒
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
            startActivity(intent);
        }
    }

    //处理扫描到的蓝牙设备
    class FoundDeviceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                //获取intent中包含的蓝牙设备
                BluetoothDevice device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                dialogAdapter.add("设备名称:"+device.getName()
                +"\n设备地址:"+device.getAddress()
                +"\n设备状态:"+(device.getBondState() == BluetoothDevice.BOND_BONDED ? "已配对"
                        : "未配对"));
                //将获取到的蓝牙设备添加到集合中去
                newDeviceSet.add(device);

            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //停止蓝牙适配器
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.disable();
        }
        if (receiver != null) {
            unregisterReceiver(receiver);
        }

    }
}
