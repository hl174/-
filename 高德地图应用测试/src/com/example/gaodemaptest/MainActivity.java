package com.example.gaodemaptest;

import java.util.List;

import com.amap.api.location.AMapLocalDayWeatherForecast;
import com.amap.api.location.AMapLocalWeatherForecast;
import com.amap.api.location.AMapLocalWeatherListener;
import com.amap.api.location.AMapLocalWeatherLive;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.location.LocationManagerProxy;
import com.amap.api.location.LocationProviderProxy;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.OnMapClickListener;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.LocationSource.OnLocationChangedListener;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends Activity implements AMapLocationListener,
		LocationSource, OnMapClickListener, AMapLocalWeatherListener {
	private MapView mapView;
	private AMap aMap;
	// 定位管理的对象
	private LocationManagerProxy mLocationManagerProxy;

	// 定位监听对象
	private OnLocationChangedListener mListener;

	// 增加一些地理围栏的对象信息
	private Marker mGPSMarker;
	private PendingIntent mPendingIntent;
	private Circle mCircle;
	public static final String GEOFENCE_BROADCAST_ACTION = "com.location.apis.geofencedemo.broadcast";

	// 定义一个广播接收对象
	private BroadcastReceiver mGeoFenceReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// 接收广播
			if (intent.getAction().equals(GEOFENCE_BROADCAST_ACTION)) {
				Bundle bundle = intent.getExtras();
				// 根据广播的status确定是在区域内还是区域外
				int status = bundle.getInt("status");
				if (status == 0) {
					Toast.makeText(getApplicationContext(), "不在区域内",
							Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(getApplicationContext(), "在区域内",
							Toast.LENGTH_SHORT).show();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mapView = (MapView) findViewById(R.id.map);
		// 重写方法
		mapView.onCreate(savedInstanceState);
		init();
	}

	/*
	 * 初始化AMap对象
	 */
	private void init() {
		if (aMap == null) {
			aMap = mapView.getMap();
		}
		/* initLocation(); */
		setUpMap();
	}

	private void setUpMap() {
		// 设置定位监听
		aMap.setLocationSource(this);
		// 设置默认定位按钮是否显示
		aMap.getUiSettings().setMyLocationButtonEnabled(true);
		// 设置为true表示显示定位层并可触发定位，false表示隐藏定位成并不可触发定位，默认是false
		aMap.setMyLocationEnabled(true);

		// 设置定位的类型为定位模式：定位（AMap.LOCATION_TYPE_LOCATE）、跟随（AMap.LOCATION_TYPE_MAP_FOLLOW）
		// 地图根据面向方向旋转（AMap.LOCATION_TYPE_MAP_ROTATE）三种模式
		aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);

		aMap.setOnMapClickListener(this);
		IntentFilter filter = new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION);
		filter.addAction(GEOFENCE_BROADCAST_ACTION);
		registerReceiver(mGeoFenceReceiver, filter);

		mLocationManagerProxy = LocationManagerProxy.getInstance(this);

		Intent intent = new Intent(GEOFENCE_BROADCAST_ACTION);
		mPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
				intent, 0);

		// 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
		// 注意设置合适的定位时间的间隔，并且在合适时间调用removeUpdates()方法来取消定位请求
		// 在定位结束后，在合适的生命周期调用destroy()方法
		// 其中如果间隔时间为-1，则定位只定一次
		mLocationManagerProxy.requestLocationData(
				LocationProviderProxy.AMapNetwork, 2000, 15, this);
		mLocationManagerProxy.requestWeatherUpdates(
				LocationManagerProxy.WEATHER_TYPE_FORECAST, this);
		MarkerOptions markOptions = new MarkerOptions();
		markOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
				.decodeResource(getResources(), R.drawable.ic_launcher)));

		mGPSMarker = aMap.addMarker(markOptions);
	}

	/*
	 * 初始化定位 (non-Javadoc)
	 */
	private void initLocation() {
		mLocationManagerProxy = LocationManagerProxy.getInstance(this);

		// 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
		// 注意设置合适的定位时间的间隔，并且在合适时间调用removeUpdates()方法来取消定位请求
		// 在定位结束后，在合适的生命周期调用destroy()方法
		// 其中如果间隔时间为-1，则定位只定一次
		mLocationManagerProxy.requestLocationData(
				LocationProviderProxy.AMapNetwork, 5000, 15, this);
		mLocationManagerProxy.setGpsEnable(false);
	}

	// 下面是一些需要重写的方法
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mapView.onResume();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mapView.onPause();
		// 对定位服务对象进行销毁
		/* mLocationManagerProxy.destroy(); */
		deactivate();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mapView.onDestroy();
		// 销毁定位
		mLocationManagerProxy.removeGeoFenceAlert(mPendingIntent);
		mLocationManagerProxy.destroy();
		unregisterReceiver(mGeoFenceReceiver);
	}

	// 下面是listener的一些抽象的类
	/*
	 * 此方法已经废弃
	 */
	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLocationChanged(AMapLocation aMapLocation) {
		// 通过aMapLocation.getAMapException()方法获取定位异常对象，再根据异常对象返回的错误码判断是否定位成功
		if (aMapLocation != null
				&& aMapLocation.getAMapException().getErrorCode() == 0) {
			// 获取位置信息
			Double geoLat = aMapLocation.getLatitude();
			Double geoLng = aMapLocation.getLongitude();

			Log.d("MainActivity", "Latitude = " + geoLat.doubleValue()
					+ ", Longitude = " + geoLng.doubleValue());

			// 通过aMapLocation.getExtras()获取位置的具体的描述信息，包括集体的省市街道等，并以空格分开
			String desc = "";
			Bundle locBundle = aMapLocation.getExtras();
			if (locBundle != null) {
				desc = locBundle.getString("desc");
				Log.d("MainActivity", "desc=" + desc);
			}
			// 显示系统小蓝点
			mListener.onLocationChanged(aMapLocation);
		}
	}

	// locationSource接口要实现的一些方法
	/*
	 * 激活定位
	 */
	@Override
	public void activate(OnLocationChangedListener onLocationChangedListener) {
		mListener = onLocationChangedListener;
		if (mLocationManagerProxy == null) {
			mLocationManagerProxy = LocationManagerProxy.getInstance(this);
			// 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
			// 注意设置合适的定位时间的间隔，并且在合适时间调用removeUpdates()方法来取消定位请求
			// 在定位结束后，在合适的生命周期调用destroy()方法
			// 其中如果间隔时间为-1，则定位只定一次
			mLocationManagerProxy.requestLocationData(
					LocationProviderProxy.AMapNetwork, -1, 10, this);
		}
	}

	/*
	 * 停止定位
	 */

	@Override
	public void deactivate() {
		mListener = null;
		if (mLocationManagerProxy != null) {
			mLocationManagerProxy.removeUpdates(this);
			mLocationManagerProxy.destroy();
		}
		mLocationManagerProxy = null;
	}

	// 重写OnMapClickListener接口，重写里面的方法
	@Override
	public void onMapClick(LatLng latLng) {
		// TODO Auto-generated method stub
		mLocationManagerProxy.removeGeoFenceAlert(mPendingIntent);
		if (mCircle != null) {
			mCircle.remove();
		}
		// 设置地理围栏，位置、半径、超过时间、处理事件
		mLocationManagerProxy.addGeoFenceAlert(latLng.latitude,
				latLng.longitude, 1000, 1000 * 60 * 30, mPendingIntent);
		// 将地理围栏添加到地图上显示
		CircleOptions circleOptions = new CircleOptions();
		circleOptions.center(latLng).radius(1000)
				.fillColor(Color.argb(180, 224, 171, 10))
				.strokeColor(Color.RED);
		mCircle = aMap.addCircle(circleOptions);
	}

	// 下面是天气相关的方法
	/*
	 * 获取未来天气的回调方法 (non-Javadoc)
	 * 
	 * @see
	 * com.amap.api.location.AMapLocalWeatherListener#onWeatherForecaseSearched
	 * (com.amap.api.location.AMapLocalWeatherForecast)
	 */
	@Override
	public void onWeatherForecaseSearched(
			AMapLocalWeatherForecast aMapLocalWeatherForecast) {
		if (aMapLocalWeatherForecast != null
				&& aMapLocalWeatherForecast.getAMapException().getErrorCode() == 0) {
			List<AMapLocalDayWeatherForecast> forcasts = aMapLocalWeatherForecast
					.getWeatherForecast();

			StringBuffer weatherForecastsString = new StringBuffer();
			for (int i = 0; i < forcasts.size(); i++) {
				AMapLocalDayWeatherForecast forcast = forcasts.get(i);
				switch (i) {
				case 0:
					// 今天天气
					// 城市
					String city = forcast.getCity();
					String today = "今天 ( " + forcast.getDate() + " )";
					String todayWeather = forcast.getDayWeather() + "    "
							+ forcast.getDayTemp() + "/"
							+ forcast.getNightTemp() + "    "
							+ forcast.getDayWindPower();
					 weatherForecastsString.append("城市：" + city + ", " + today + ", 天气信息：" + todayWeather);
					break;
				// 明天天气
				case 1:

					String tomorrow = "明天 ( " + forcast.getDate() + " )";
					String tomorrowWeather = forcast.getDayWeather() + "    "
							+ forcast.getDayTemp() + "/"
							+ forcast.getNightTemp() + "    "
							+ forcast.getDayWindPower();
					 weatherForecastsString.append("; " + tomorrow + ", 天气信息：" + tomorrowWeather);
					break;
				// 后天天气
				case 2:

					String aftertomorrow = "后天( " + forcast.getDate() + " )";
					String aftertomorrowWeather = forcast.getDayWeather()
							+ "    " + forcast.getDayTemp() + "/"
							+ forcast.getNightTemp() + "    "
							+ forcast.getDayWindPower();
					 weatherForecastsString.append("; " + aftertomorrow + ", 天气信息：" + aftertomorrowWeather);
					break;
				}
			}
			 Toast.makeText(this, "天气预报: " + weatherForecastsString, Toast.LENGTH_SHORT).show();

		} else {
			// 获取天气预报失败
			Toast.makeText(
					this,
					"获取天气预报失败:"
							+ aMapLocalWeatherForecast.getAMapException()
									.getErrorMessage(), Toast.LENGTH_SHORT)
					.show();
		}

	}

	/*
	 * 获取实时的天气回调方法
	 */
	@Override
	public void onWeatherLiveSearched(AMapLocalWeatherLive aMapLocalWeatherLive) {
		if (aMapLocalWeatherLive != null
				&& aMapLocalWeatherLive.getAMapException().getErrorCode() == 0) {
			String city = aMapLocalWeatherLive.getCity();// 城市
			String weather = aMapLocalWeatherLive.getWeather();// 天气情况
			String windDir = aMapLocalWeatherLive.getWindDir();// 风向
			String windPower = aMapLocalWeatherLive.getWindPower();// 风力
			String humidity = aMapLocalWeatherLive.getHumidity();// 空气湿度
			String reportTime = aMapLocalWeatherLive.getReportTime();// 数据发布时间

			String weatherInfo = "城市：" + city + ", 天气情况：" + weather + ", 风向："
					+ windDir + ", 风力：" + windPower + ", 空气湿度：" + humidity
					+ ", 数据发布时间：" + reportTime;

			Log.d("MainActivity", weatherInfo);
			Toast.makeText(this, weatherInfo, Toast.LENGTH_LONG).show();
		} else {
			// 获取天气预报失败
			Toast.makeText(
					this,
					"获取实时天气失败:"
							+ aMapLocalWeatherLive.getAMapException()
									.getErrorMessage(), Toast.LENGTH_SHORT)
					.show();
		}
	}
}
