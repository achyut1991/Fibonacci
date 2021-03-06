package com.icreate.projectx.task;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.icreate.projectx.ProjectXPreferences;
import com.icreate.projectx.R;
import com.icreate.projectx.homeActivity;
import com.icreate.projectx.datamodel.PriorityEnum;
import com.icreate.projectx.datamodel.ProjectxGlobalState;
import com.icreate.projectx.datamodel.Task;
import com.icreate.projectx.datamodel.TaskList;
import com.icreate.projectx.net.GetProjectTask;
import com.icreate.projectx.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.icreate.projectx.pulltorefresh.library.PullToRefreshListView;

public class TaskListActivity extends Activity {
	private TextView logoText;
	private ProjectxGlobalState globalState;
	private ListView TaskListView;
	private myTasksBaseAdapter mytasksAdapter;
	private PullToRefreshListView taskListViewWrapper;
	private Context cont;
	private AlertDialog alert;
	private ArrayList<Task> tasks;
	private final ArrayList<Task> filteredTasks = new ArrayList<Task>();
	private Activity currentActivity;
	private Button myTaskSearchButton;
	private TextView myTaskSearch;
	private String passedUserId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.tasklist);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.logo1);

		cont = this;
		currentActivity = this;

		globalState = (ProjectxGlobalState) getApplication();

		Typeface font = Typeface.createFromAsset(getAssets(), "EraserDust.ttf");
		logoText = (TextView) findViewById(R.id.logoText);
		logoText.setTypeface(font);
		// logoText.setTextColor(R.color.white);

		myTaskSearchButton = (Button) findViewById(R.id.mytaskSearchButton);
		myTaskSearch = (TextView) findViewById(R.id.mytaskSearch);
		myTaskSearch.setTypeface(font);
		ImageButton homeButton = (ImageButton) findViewById(R.id.logoImageButton);
		homeButton.setBackgroundResource(R.drawable.home_button);

		homeButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent HomeIntent = new Intent(cont, homeActivity.class);
				HomeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(HomeIntent);
				currentActivity.finish();

			}
		});

		final CharSequence[] items = { "Earliest Due", "Priority" };

		AlertDialog.Builder builder = new AlertDialog.Builder(cont);
		builder.setTitle("Sort By");
		builder.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item) {
				myTaskSearch.setText("");
				switch (item) {
				case 0:
					Collections.sort(tasks, new MyTaskDueDateComparable());
					Collections.reverse(tasks);
					break;
				case 1:
					Collections.sort(tasks, new MyTaskPriorityComparable());
					break;
				}
				mytasksAdapter = new myTasksBaseAdapter(cont, tasks);
				TaskListView.setAdapter(mytasksAdapter);
			}
		});
		alert = builder.create();

		taskListViewWrapper = (PullToRefreshListView) findViewById(R.id.taskListView);
		TaskListView = taskListViewWrapper.getRefreshableView();
		TaskListView.setTextFilterEnabled(true);
		registerForContextMenu(TaskListView);

		Bundle extras = getIntent().getExtras();
		passedUserId = null;
		if (extras != null) {
			passedUserId = extras.getString("requiredId");
			if (passedUserId.equalsIgnoreCase(ProjectXPreferences.readString(cont, ProjectXPreferences.USER, globalState.getUserid()))) {
				logoText.setText("My Tasks");
			} else {
				logoText.setText("Tasks");
			}

			passedUserId = extras.getString("requiredId");
			String url = ProjectxGlobalState.urlPrefix + "TaskList.php";
			List<NameValuePair> params = new LinkedList<NameValuePair>();
			params.add(new BasicNameValuePair("user_id", passedUserId));
			String paramString = URLEncodedUtils.format(params, "utf-8");
			url += "?" + paramString;
			ProgressDialog dialog = new ProgressDialog(cont);
			dialog.setMessage("Getting Tasks");
			dialog.setCancelable(false);
			dialog.setCanceledOnTouchOutside(false);
			ListTask ListTasks = new ListTask(cont, currentActivity, dialog, TaskListView);
			ListTasks.execute(url);
		}

		taskListViewWrapper.setOnRefreshListener(new OnRefreshListener() {

			@Override
			public void onRefresh() {
				if (passedUserId != null) {
					myTaskSearch.setText("");
					String url = ProjectxGlobalState.urlPrefix + "TaskList.php";
					List<NameValuePair> params = new LinkedList<NameValuePair>();
					params.add(new BasicNameValuePair("user_id", passedUserId));
					String paramString = URLEncodedUtils.format(params, "utf-8");
					url += "?" + paramString;
					ListTask listTask = new ListTask(cont, currentActivity, null, TaskListView);
					listTask.execute(url);
				}
			}
		});

		myTaskSearch.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				int textLength2 = myTaskSearch.getText().length();
				if (tasks != null) {
					filteredTasks.clear();
					for (int i = 0; i < tasks.size(); i++) {
						Log.d("YOLO", tasks.get(i).getTask_name());
						if (textLength2 <= tasks.get(i).getTask_name().length()) {
							if (myTaskSearch.getText().toString().equalsIgnoreCase((String) tasks.get(i).getTask_name().subSequence(0, textLength2))) {
								filteredTasks.add(tasks.get(i));
							}
						}
					}
					mytasksAdapter = new myTasksBaseAdapter(cont, filteredTasks);
					TaskListView.setAdapter(mytasksAdapter);
				}
			}
		});

		myTaskSearchButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (tasks != null)
					alert.show();
			}
		});

		TaskListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Object o = TaskListView.getItemAtPosition(position);
				Task selectedTask = (Task) o;

				Intent TaskViewIntent = new Intent(cont, TaskViewActivity.class);
				int projectId = selectedTask.getProjectId();
				String url = ProjectxGlobalState.urlPrefix + "getProject.php?project_id=" + projectId;
				ProgressDialog dialog = new ProgressDialog(cont);
				dialog.setMessage("Getting Project Info...");
				dialog.setCancelable(false);
				dialog.setCanceledOnTouchOutside(false);
				dialog.show();
				GetProjectTask getProjectTask = new GetProjectTask(cont, currentActivity, dialog, selectedTask.getTask_id(), false);
				getProjectTask.execute(url);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		myTaskSearch.setText("");
	}

	public class ListTask extends AsyncTask<String, Void, String> {
		private final Context context;
		private final Activity callingActivity;
		private final ProgressDialog dialog;
		private final ListView taskListView;

		public ListTask(Context context, Activity callingActivity, ProgressDialog dialog, ListView taskListView) {
			this.context = context;
			this.callingActivity = callingActivity;
			this.dialog = dialog;
			this.taskListView = taskListView;
		}

		@Override
		protected void onPreExecute() {
			if (dialog != null) {
				if (!this.dialog.isShowing()) {
					this.dialog.setMessage("Getting Tasks...");
					this.dialog.show();
					this.dialog.setCanceledOnTouchOutside(false);
					this.dialog.setCancelable(false);
				}
			}
		}

		@Override
		protected String doInBackground(String... urls) {
			String response = "";
			for (String url : urls) {
				HttpClient client = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(url);
				try {
					HttpResponse execute = client.execute(httpGet);
					InputStream content = execute.getEntity().getContent();

					BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
					String s = "";
					while ((s = buffer.readLine()) != null) {
						response += s;
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return response;
		}

		@Override
		protected void onPostExecute(String result) {
			if (dialog != null) {
				if (this.dialog.isShowing()) {
					this.dialog.dismiss();
				}
			}
			try {
				JSONObject resultJson = new JSONObject(result);
				Log.d("TaskList", resultJson.toString());
				if (resultJson.getString("msg").equals("success")) {
					Gson gson = new Gson();
					TaskList tasksContainer = gson.fromJson(result, TaskList.class);
					globalState.setTaskList(tasksContainer);
					tasks = tasksContainer.getTasks();
					mytasksAdapter = new myTasksBaseAdapter(context, tasks);
					taskListView.setAdapter(mytasksAdapter);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mytasksAdapter.notifyDataSetChanged();
						}
					});
					if (dialog == null) {
						taskListViewWrapper.onRefreshComplete();
					}
				} else {

				}
			} catch (JSONException e) {

				e.printStackTrace();
			}
		}
	}

	public class MyTaskDueDateComparable implements Comparator<Task> {

		@Override
		public int compare(Task o1, Task o2) {
			return (o2.getDue_date().compareTo(o1.getDue_date()));
		}
	}

	public class MyTaskPriorityComparable implements Comparator<Task> {

		@Override
		public int compare(Task o1, Task o2) {
			int pos1 = PriorityEnum.valueOf(o1.getTask_priority()).ordinal();
			int pos2 = PriorityEnum.valueOf(o2.getTask_priority()).ordinal();
			if (pos1 == pos2)
				return 0;
			return (pos1 > pos2 ? -1 : 1);
		}
	}
}
