package com.icreate.projectx;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.icreate.projectx.datamodel.Project;
import com.icreate.projectx.datamodel.ProjectList;
import com.icreate.projectx.datamodel.ProjectxGlobalState;

public class ProjectListActivity extends Activity {
	private TextView logoText;
	private ProjectxGlobalState globalState;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.projectlist);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.logo1);

		final Context cont = this;
		final Activity currentActivity = this;

		globalState = (ProjectxGlobalState) getApplication();

		Typeface font = Typeface.createFromAsset(getAssets(), "EraserDust.ttf");
		logoText = (TextView) findViewById(R.id.logoText);
		logoText.setTypeface(font);
		logoText.setTextColor(R.color.white);

		ImageButton homeButton = (ImageButton) findViewById(R.id.logoImageButton);
		homeButton.setBackgroundResource(R.drawable.home_button);

		homeButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				startActivity(new Intent(cont, homeActivity.class));

			}
		});

		final ListView projectListView = (ListView) findViewById(R.id.ListView01);
		projectListView.setTextFilterEnabled(true);

		Bundle extras = getIntent().getExtras();
		String passedUserId = null;
		if (extras != null) {
			passedUserId = extras.getString("requiredId");
			if (passedUserId.equalsIgnoreCase(globalState.getUserid())) {
				logoText.setText("My Projects");
			} else {
				logoText.setText("Projects");
			}

			Toast.makeText(cont, extras.getString("requiredId"),
					Toast.LENGTH_LONG).show();
			passedUserId = extras.getString("requiredId");
			String url = "http://ec2-54-251-4-64.ap-southeast-1.compute.amazonaws.com/api/projectList.php";
			List<NameValuePair> params = new LinkedList<NameValuePair>();
			params.add(new BasicNameValuePair("user_id", passedUserId));
			String paramString = URLEncodedUtils.format(params, "utf-8");
			url += "?" + paramString;
			ProgressDialog dialog = new ProgressDialog(cont);
			dialog.setMessage("Getting Projects");
			ProjectListTask projectListTask = new ProjectListTask(cont,
					currentActivity, dialog, projectListView);
			System.out.println(url);
			projectListTask.execute(url);
		}

		projectListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Object o = projectListView.getItemAtPosition(position);
				Project selectedProject = (Project) o;
				Toast.makeText(
						cont,
						"You have chosen: " + " "
								+ selectedProject.getProject_name() + " "
								+ selectedProject.getProject_id(),
								
						Toast.LENGTH_LONG).show();
				Intent projectViewIntent = new Intent(cont,
						projectViewActivity.class);
				//String currentUserId = globalData.getUserid();
			//	if (!(currentUserId.isEmpty())) {
					projectViewIntent.putExtra("projectid",selectedProject.getProject_id() );
				//}
				//System.out.println(fullObject.getProject_name());
				startActivity(projectViewIntent);
			}
		});
	}

	public class ProjectListTask extends AsyncTask<String, Void, String> {
		private final Context context;
		private final Activity callingActivity;
		private final ProgressDialog dialog;
		private final ListView projectListView;

		public ProjectListTask(Context context, Activity callingActivity,
				ProgressDialog dialog, ListView projectListView) {
			this.context = context;
			this.callingActivity = callingActivity;
			this.dialog = dialog;
			this.projectListView = projectListView;
		}

		@Override
		protected void onPreExecute() {
			if (!this.dialog.isShowing()) {
				this.dialog.setMessage("Getting Projects...");
				this.dialog.show();
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

					BufferedReader buffer = new BufferedReader(
							new InputStreamReader(content));
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
			if (this.dialog.isShowing()) {
				this.dialog.dismiss();
			}
			System.out.println(result);
			try {
				JSONObject resultJson = new JSONObject(result);
				Log.d("ProjectList", resultJson.toString());
				if (resultJson.getString("msg").equals("success")) {
					Gson gson = new Gson();
					ProjectList projectsContainer = gson.fromJson(result,
							ProjectList.class);
					ArrayList<Project> projects = projectsContainer
							.getProjects();
					projectListView.setAdapter(new ProjectListBaseAdapter(
							context, projects));
					for (Project project : projects) {
						System.out.println(project.getLeader_name());
						System.out.println(project.getProject_name());
					}
				} else {
					Toast.makeText(context, "Project Lists empty",
							Toast.LENGTH_LONG).show();
				}
			} catch (JSONException e) {
				Toast.makeText(context, R.string.server_error,
						Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
		}
	}
}