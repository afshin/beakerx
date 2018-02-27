/*
 *  Copyright 2018 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.twosigma.beakerx.widget;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.scheduler.SparkListener;
import org.apache.spark.scheduler.SparkListenerApplicationEnd;
import org.apache.spark.scheduler.SparkListenerApplicationStart;
import org.apache.spark.scheduler.SparkListenerJobEnd;
import org.apache.spark.scheduler.SparkListenerJobStart;
import org.apache.spark.scheduler.SparkListenerStageCompleted;
import org.apache.spark.scheduler.SparkListenerStageSubmitted;
import org.apache.spark.scheduler.SparkListenerTaskEnd;
import org.apache.spark.scheduler.SparkListenerTaskStart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class SparkContextManager {

  private final SparkContext sparkContext;
  private Map<Integer, VBox> jobs = new HashMap<>();
  private Map<Integer, IntProgress> progressBars = new HashMap<>();
  private Map<Integer, Label> labels = new HashMap<>();
  private VBox jobPanel = null;

  public SparkContextManager(SparkConf sparkConf) {
    this.sparkContext = create(sparkConf);
  }

  public SparkContext getSparkContext() {
    return sparkContext;
  }

  private SparkContext create(SparkConf sparkConf) {
    SparkContext sc = new SparkContext(sparkConf);
    sc.addSparkListener(new SparkListener() {

      @Override
      public void onJobStart(SparkListenerJobStart jobStart) {
        super.onJobStart(jobStart);
        startJob(jobStart.jobId());
      }

      @Override
      public void onJobEnd(SparkListenerJobEnd jobEnd) {
        super.onJobEnd(jobEnd);
        endJob(jobEnd.jobId());
      }

      @Override
      public void onStageSubmitted(SparkListenerStageSubmitted stageSubmitted) {
        super.onStageSubmitted(stageSubmitted);
        startStage(stageSubmitted.stageInfo().stageId(), stageSubmitted.stageInfo().numTasks());
      }

      @Override
      public void onStageCompleted(SparkListenerStageCompleted stageCompleted) {
        super.onStageCompleted(stageCompleted);
        endStage(stageCompleted.stageInfo().stageId());
      }

      @Override
      public void onTaskStart(SparkListenerTaskStart taskStart) {
        super.onTaskStart(taskStart);
      }

      @Override
      public void onTaskEnd(SparkListenerTaskEnd taskEnd) {
        super.onTaskEnd(taskEnd);
        taskEnd(taskEnd.stageId(), taskEnd.taskInfo().taskId());
      }

      @Override
      public void onApplicationStart(SparkListenerApplicationStart applicationStart) {
        super.onApplicationStart(applicationStart);
      }

      @Override
      public void onApplicationEnd(SparkListenerApplicationEnd applicationEnd) {
        super.onApplicationEnd(applicationEnd);
      }
    });
    return sc;
  }

  private void startJob(int jobId) {
    jobPanel = createJobPanel();
    VBox job = createJob(jobId);
    jobs.put(jobId, job);
    jobPanel.add(job);
    jobPanel.display();
  }

  private VBox createJobPanel() {
    VBox jobPanel = new VBox();
    jobs.values().forEach(x -> jobPanel.add(x));
    return jobPanel;
  }

  private VBox createJob(int jobId) {
    HTML jobLink = jobLink(jobId);
    List<Widget> jobItems = new ArrayList<>();
    jobItems.add(jobLink);
    return new VBox(jobItems);
  }

  private void endJob(int jobId) {

  }

  private void startStage(int stageId, int numTasks) {
    IntProgress intProgress = new IntProgress(0, numTasks, 1);
    intProgress.setBarStyle(IntProgress.BarStyle.INFO);
    Label label = new Label();
    label.setValue(intProgress.getValue() + "/" + intProgress.getMax());
    VBox job = jobs.get(stageId);
    HBox stageWithProgress = new HBox(asList(stageLink(stageId), intProgress, label));
    stageWithProgress.getLayout().setMargin("0 0 0 40px");
    job.add(stageWithProgress);
    progressBars.put(stageId, intProgress);
    labels.put(stageId, label);
  }

  private void endStage(int stageId) {
    IntProgress intProgress = progressBars.get(stageId);
    intProgress.setBarStyle(IntProgress.BarStyle.SUCCESS);
    jobPanel.getLayout().setDisplayNone();
    jobPanel.close();
    jobPanel = null;
  }

  private void taskEnd(int stageId, long taskId) {
    IntProgress intProgress = progressBars.get(stageId);
    intProgress.setValue(intProgress.getValue() + 1);
    labels.get(stageId).setValue(intProgress.getValue() + "/" + intProgress.getMax());
  }

  private HTML stageLink(int stageId) {
    if (sparkContext.uiWebUrl().isDefined()) {
      HTML html = new HTML();
      html.setValue("<a target=\"_blank\" href=\"" + sparkContext.uiWebUrl().get() + "/stages/stage/?id=" + stageId + "&attempt=0\">Stage " + stageId + "</a>");
      return html;
    } else {
      HTML html = new HTML();
      html.setValue("<a target=\"_blank\" href=\"\">Spark job " + stageId + "</a>");
      return html;
    }
  }

  private HTML jobLink(int jobId) {
    if (sparkContext.uiWebUrl().isDefined()) {
      HTML html = new HTML();
      html.setValue("<a target=\"_blank\" href=\"" + sparkContext.uiWebUrl().get() + "/jobs/job/?id=" + jobId + "\">Spark job " + jobId + "</a>");
      return html;
    } else {
      HTML html = new HTML();
      html.setValue("<a target=\"_blank\" href=\"\">Spark job " + jobId + "</a>");
      return html;
    }
  }


}
