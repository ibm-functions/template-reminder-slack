/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package packages

import common.TestUtils.RunResult
import common._
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner
import java.io._

import io.restassured.RestAssured
import io.restassured.config.SSLConfig
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class ReminderSlackTests extends TestHelpers with WskTestHelpers with BeforeAndAfterAll with WskActorSystem {

  implicit val wskprops = WskProps()
  val wsk = new Wsk()
  val allowedActionDuration = 120 seconds

  // statuses for deployWeb
  val successStatus =
    """"status": "success""""

  val deployTestRepo = "https://github.com/ibm-functions/template-reminder-slack"
  val slackReminderAction = "send-message"
  val slackSequence = "post_message_slack_sequence"
  val packageName = "myPackage"
  val triggerName = "myTrigger"
  val ruleName = "myRule"
  val binding = "openwhisk-alarms"
  val fakeAlarmAction = "openwhisk-alarms/alarm"
  val deployAction = "/whisk.system/deployWeb/wskdeploy"
  val deployActionURL = s"https://${wskprops.apihost}/api/v1/web${deployAction}.http"

  //set parameters for deploy tests
  val nodejsRuntimePath = "runtimes/nodejs"
  val nodejsfolder = "runtimes/nodejs/actions"
  val nodejskind = "nodejs:12"
  val phpRuntimePath = "runtimes/php"
  val phpfolder = "runtimes/php/actions"
  val phpkind = "php:7.4"
  val pythonRuntimePath = "runtimes/python"
  val pythonfolder = "runtimes/python/actions"
  val pythonkind = "python:3.9"
  val swiftRuntimePath = "runtimes/swift"
  val swiftfolder = "runtimes/swift/actions"
  val swiftkind = "swift:4.2"

  behavior of "Get Slack Reminder Template"

  // test to create the nodejs 10 slack reminder template from github url.  Will use preinstalled folder.
  it should "create the nodejs 10 slack reminder action from github url" in {
    // create unique asset names
    val timestamp: String = System.currentTimeMillis.toString
    val nodejsPackage = packageName + timestamp
    val nodejsTrigger = triggerName + timestamp
    val nodejsRule = ruleName + timestamp
    val nodejsSlackAction = nodejsPackage + "/" + slackReminderAction
    val nodejsSlackSequence = nodejsPackage + "/" + slackSequence

    makePostCallWithExpectedResult(
      JsObject(
        "gitUrl" -> JsString(deployTestRepo),
        "manifestPath" -> JsString(nodejsRuntimePath),
        "envData" -> JsObject(
          "PACKAGE_NAME" -> JsString(nodejsPackage),
          "SLACK_WEBHOOK_URL" -> JsString("https://slack.com/"),
          "ALARM_CRON" -> JsString("1 * * *"),
          "RULE_NAME" -> JsString(nodejsRule),
          "TRIGGER_NAME" -> JsString(nodejsTrigger)),
        "wskApiHost" -> JsString(wskprops.apihost),
        "wskAuth" -> JsString(wskprops.authKey)),
      successStatus,
      200)

    // check that both actions were created and can be invoked with expected results
    withActivation(wsk.activation, wsk.action.invoke(nodejsSlackAction)) {
      _.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
    }

    withActivation(wsk.activation, wsk.action.invoke(fakeAlarmAction, Map("message" -> "echo".toJson))) {
      _.response.result.get.toString should include("echo")
    }

    // confirm trigger exists
    val triggers = wsk.trigger.list()
    verifyTriggerList(triggers, nodejsTrigger);
    val triggerRun = wsk.trigger.fire(nodejsTrigger)

    // confirm trigger will fire sequence with expected result
    withActivation(wsk.activation, triggerRun) { activation =>
      val logEntry = activation.logs.get(0).parseJson.asJsObject
      val triggerActivationId: String = logEntry.getFields("activationId")(0).convertTo[String]
      withActivation(wsk.activation, triggerActivationId) { triggerActivation =>
        triggerActivation.response.result.get.toString should include(
          "Your scrum is starting now.  Time to find your team!");
      }
    }

    // confirm rule exists
    val rules = wsk.rule.list()
    verifyRuleList(rules, nodejsRule)

    val action = wsk.action.get(nodejsSlackAction)
    verifyAction(action, nodejsSlackAction, JsString(nodejskind))

    // check that sequence was created and is invoked with expected results
    val runSequence = wsk.action.invoke(nodejsSlackSequence)
    withActivation(wsk.activation, runSequence, totalWait = 2 * allowedActionDuration) { activation =>
      checkSequenceLogs(activation, 2)
      activation.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
    }

    // clean up after test
    wsk.action.delete(nodejsSlackAction)
    wsk.action.delete(nodejsSlackSequence)
    wsk.pkg.delete(binding)
    wsk.pkg.delete(nodejsPackage)
    wsk.trigger.delete(nodejsTrigger)
    wsk.rule.delete(nodejsRule)
  }

  // test to create the php slack reminder template from github url.  Will use preinstalled folder.
  it should "create the php slack reminder action from github url" in {
    // create unique asset names
    val timestamp: String = System.currentTimeMillis.toString
    val phpPackage = packageName + timestamp
    val phpTrigger = triggerName + timestamp
    val phpRule = ruleName + timestamp
    val phpSlackAction = phpPackage + "/" + slackReminderAction
    val phpSlackSequence = phpPackage + "/" + slackSequence

    makePostCallWithExpectedResult(
      JsObject(
        "gitUrl" -> JsString(deployTestRepo),
        "manifestPath" -> JsString(phpRuntimePath),
        "envData" -> JsObject(
          "PACKAGE_NAME" -> JsString(phpPackage),
          "SLACK_WEBHOOK_URL" -> JsString("https://slack.com/"),
          "ALARM_CRON" -> JsString("1 * * *"),
          "RULE_NAME" -> JsString(phpRule),
          "TRIGGER_NAME" -> JsString(phpTrigger)),
        "wskApiHost" -> JsString(wskprops.apihost),
        "wskAuth" -> JsString(wskprops.authKey)),
      successStatus,
      200)

    // check that both actions were created and can be invoked with expected results
    withActivation(wsk.activation, wsk.action.invoke(phpSlackAction)) {
      _.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
    }

    withActivation(wsk.activation, wsk.action.invoke(fakeAlarmAction, Map("message" -> "echo".toJson))) {
      _.response.result.get.toString should include("echo")
    }

    // confirm trigger exists
    val triggers = wsk.trigger.list()
    verifyTriggerList(triggers, phpTrigger);
    val triggerRun = wsk.trigger.fire(phpTrigger)

    // confirm trigger will fire sequence with expected result
    withActivation(wsk.activation, triggerRun) { activation =>
      val logEntry = activation.logs.get(0).parseJson.asJsObject
      val triggerActivationId: String = logEntry.getFields("activationId")(0).convertTo[String]
      withActivation(wsk.activation, triggerActivationId) { triggerActivation =>
        triggerActivation.response.result.get.toString should include(
          "Your scrum is starting now.  Time to find your team!");
      }
    }

    // confirm rule exists
    val rules = wsk.rule.list()
    verifyRuleList(rules, phpRule)

    val action = wsk.action.get(phpSlackAction)
    verifyAction(action, phpSlackAction, JsString(phpkind))

    // check that sequence was created and is invoked with expected results
    val runSequence = wsk.action.invoke(phpSlackSequence)
    withActivation(wsk.activation, runSequence, totalWait = 2 * allowedActionDuration) { activation =>
      checkSequenceLogs(activation, 2)
      activation.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
    }

    // clean up after test
    wsk.action.delete(phpSlackAction)
    wsk.action.delete(phpSlackSequence)
    wsk.pkg.delete(binding)
    wsk.pkg.delete(phpPackage)
    wsk.trigger.delete(phpTrigger)
    wsk.rule.delete(phpRule)
  }

  // test to create the python slack reminder template from github url.  Will use preinstalled folder.
  it should "create the python slack reminder action from github url" in {
    // create unique asset names
    val timestamp: String = System.currentTimeMillis.toString
    val pythonPackage = packageName + timestamp
    val pythonTrigger = triggerName + timestamp
    val pythonRule = ruleName + timestamp
    val pythonSlackAction = pythonPackage + "/" + slackReminderAction
    val pythonSlackSequence = pythonPackage + "/" + slackSequence

    makePostCallWithExpectedResult(
      JsObject(
        "gitUrl" -> JsString(deployTestRepo),
        "manifestPath" -> JsString(pythonRuntimePath),
        "envData" -> JsObject(
          "PACKAGE_NAME" -> JsString(pythonPackage),
          "SLACK_WEBHOOK_URL" -> JsString("https://slack.com/"),
          "ALARM_CRON" -> JsString("1 * * *"),
          "RULE_NAME" -> JsString(pythonRule),
          "TRIGGER_NAME" -> JsString(pythonTrigger)),
        "wskApiHost" -> JsString(wskprops.apihost),
        "wskAuth" -> JsString(wskprops.authKey)),
      successStatus,
      200)

    // check that both actions were created and can be invoked with expected results
    withActivation(wsk.activation, wsk.action.invoke(pythonSlackAction)) {
      _.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
    }

    withActivation(wsk.activation, wsk.action.invoke(fakeAlarmAction, Map("message" -> "echo".toJson))) {
      _.response.result.get.toString should include("echo")
    }

    // confirm trigger exists
    val triggers = wsk.trigger.list()
    verifyTriggerList(triggers, pythonTrigger);
    val triggerRun = wsk.trigger.fire(pythonTrigger)

    // confirm trigger will fire sequence with expected result
    withActivation(wsk.activation, triggerRun) { activation =>
      val logEntry = activation.logs.get(0).parseJson.asJsObject
      val triggerActivationId: String = logEntry.getFields("activationId")(0).convertTo[String]
      withActivation(wsk.activation, triggerActivationId) { triggerActivation =>
        triggerActivation.response.result.get.toString should include(
          "Your scrum is starting now.  Time to find your team!");
      }
    }

    // confirm rule exists
    val rules = wsk.rule.list()
    verifyRuleList(rules, pythonRule)

    val action = wsk.action.get(pythonSlackAction)
    verifyAction(action, pythonSlackAction, JsString(pythonkind))

    // check that sequence was created and is invoked with expected results
    val runSequence = wsk.action.invoke(pythonSlackSequence)
    withActivation(wsk.activation, runSequence, totalWait = 2 * allowedActionDuration) { activation =>
      checkSequenceLogs(activation, 2)
      activation.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
    }

    // clean up after test
    wsk.action.delete(pythonSlackAction)
    wsk.action.delete(pythonSlackSequence)
    wsk.pkg.delete(binding)
    wsk.pkg.delete(pythonPackage)
    wsk.trigger.delete(pythonTrigger)
    wsk.rule.delete(pythonRule)
  }

  // test to create the swift slack reminder template from github url.  Will use preinstalled folder.
  it should "create the swift slack reminder action from github url" in {
    // create unique asset names
    val timestamp: String = System.currentTimeMillis.toString
    val swiftPackage = packageName + timestamp
    val swiftTrigger = triggerName + timestamp
    val swiftRule = ruleName + timestamp
    val swiftSlackAction = swiftPackage + "/" + slackReminderAction
    val swiftSlackSequence = swiftPackage + "/" + slackSequence

    makePostCallWithExpectedResult(
      JsObject(
        "gitUrl" -> JsString(deployTestRepo),
        "manifestPath" -> JsString(swiftRuntimePath),
        "envData" -> JsObject(
          "PACKAGE_NAME" -> JsString(swiftPackage),
          "SLACK_WEBHOOK_URL" -> JsString("https://slack.com/"),
          "ALARM_CRON" -> JsString("1 * * *"),
          "RULE_NAME" -> JsString(swiftRule),
          "TRIGGER_NAME" -> JsString(swiftTrigger)),
        "wskApiHost" -> JsString(wskprops.apihost),
        "wskAuth" -> JsString(wskprops.authKey)),
      successStatus,
      200)

    // check that both actions were created and can be invoked with expected results
    withActivation(wsk.activation, wsk.action.invoke(swiftSlackAction)) {
      _.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
    }

    withActivation(wsk.activation, wsk.action.invoke(fakeAlarmAction, Map("message" -> "echo".toJson))) {
      _.response.result.get.toString should include("echo")
    }

    // confirm trigger exists
    val triggers = wsk.trigger.list()
    verifyTriggerList(triggers, swiftTrigger);
    val triggerRun = wsk.trigger.fire(swiftTrigger)

    // confirm trigger will fire sequence with expected result
    withActivation(wsk.activation, triggerRun) { activation =>
      val logEntry = activation.logs.get(0).parseJson.asJsObject
      val triggerActivationId: String = logEntry.getFields("activationId")(0).convertTo[String]
      withActivation(wsk.activation, triggerActivationId) { triggerActivation =>
        triggerActivation.response.result.get.toString should include(
          "Your scrum is starting now.  Time to find your team!");
      }
    }

    // confirm rule exists
    val rules = wsk.rule.list()
    verifyRuleList(rules, swiftRule)

    val action = wsk.action.get(swiftSlackAction)
    verifyAction(action, swiftSlackAction, JsString(swiftkind))

    // check that sequence was created and is invoked with expected results
    val runSequence = wsk.action.invoke(swiftSlackSequence)
    withActivation(wsk.activation, runSequence, totalWait = 2 * allowedActionDuration) { activation =>
      checkSequenceLogs(activation, 2)
      activation.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
    }

    // clean up after test
    wsk.action.delete(swiftSlackAction)
    wsk.action.delete(swiftSlackSequence)
    wsk.pkg.delete(binding)
    wsk.pkg.delete(swiftPackage)
    wsk.trigger.delete(swiftTrigger)
    wsk.rule.delete(swiftRule)
  }

  /**
   * Test the nodejs 10 "Get Slack Reminder Template" template
   */
  it should "invoke nodejs 10 send-message.js and get the result" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val timestamp: String = System.currentTimeMillis.toString
    val name = "messageNodeJS" + timestamp
    val file = Some(new File(nodejsfolder, "send-message.js").toString())

    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, file, kind = Some(nodejskind))
    }

    withActivation(wsk.activation, wsk.action.invoke(name)) { activation =>
      activation.response.success shouldBe true
      activation.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
    }
  }

  /**
   * Test the php "Get Slack Reminder Template" template
   */
  it should "invoke send-message.php and get the result" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val timestamp: String = System.currentTimeMillis.toString
    val name = "messagePhp" + timestamp
    val file = Some(new File(phpfolder, "send-message.php").toString())
    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, file, kind = Some(phpkind))
    }

    withActivation(wsk.activation, wsk.action.invoke(name)) { activation =>
      activation.response.success shouldBe true
      activation.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
    }
  }

  /**
   * Test the python "Get Slack Reminder Template" template
   */
  it should "invoke send-message.py and get the result" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val timestamp: String = System.currentTimeMillis.toString
    val name = "messagePython" + timestamp
    val file = Some(new File(pythonfolder, "send-message.py").toString())
    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, file, kind = Some(pythonkind))
    }

    withActivation(wsk.activation, wsk.action.invoke(name)) { activation =>
      activation.response.success shouldBe true
      activation.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
    }
  }

  /**
   * Test the swift "Get Slack Reminder Template" template
   */
  it should "invoke send-message.swift and get the result" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val timestamp: String = System.currentTimeMillis.toString
    val name = "messageSwift" + timestamp
    val file = Some(new File(swiftfolder, "send-message.swift").toString())
    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, file, kind = Some(swiftkind))
    }
    withActivation(wsk.activation, wsk.action.invoke(name)) { activation =>
      activation.response.success shouldBe true
      activation.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
    }
  }

  /**
   * checks logs for the activation of a sequence (length/size and ids)
   */
  private def checkSequenceLogs(activation: ActivationResult, size: Int) = {
    activation.logs shouldBe defined
    // check that the logs are what they are supposed to be (activation ids)
    activation.logs.get.size shouldBe (size) // the number of activations in this sequence
  }

  private def makePostCallWithExpectedResult(params: JsObject, expectedResult: String, expectedCode: Int) = {
    val response = RestAssured
      .given()
      .contentType("application/json\r\n")
      .config(RestAssured.config().sslConfig(new SSLConfig().relaxedHTTPSValidation()))
      .body(params.toString())
      .post(deployActionURL)
    assert(response.statusCode() == expectedCode)
    response.body.asString should include(expectedResult)
    response.body.asString.parseJson.asJsObject.getFields("activationId") should have length 1
  }

  private def verifyRuleList(ruleListResult: RunResult, ruleName: String) = {
    val ruleList = ruleListResult.stdout
    val listOutput = ruleList.lines
    listOutput.find(_.contains(ruleName)).get should (include(ruleName) and include("active"))
  }

  private def verifyTriggerList(triggerListResult: RunResult, triggerName: String) = {
    val triggerList = triggerListResult.stdout
    val listOutput = triggerList.lines
    listOutput.find(_.contains(triggerName)).get should include(triggerName)
  }

  private def verifyAction(action: RunResult, name: String, kindValue: JsString): Unit = {
    val stdout = action.stdout
    assert(stdout.startsWith(s"ok: got action $name\n"))
    wsk.parseJsonString(stdout).fields("exec").asJsObject.fields("kind") shouldBe kindValue
  }
}
