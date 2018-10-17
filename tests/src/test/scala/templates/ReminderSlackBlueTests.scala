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

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner
import common._
import common.TestUtils.RunResult
import com.jayway.restassured.RestAssured
import com.jayway.restassured.config.SSLConfig
import java.io._

import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class ReminderSlackBlueTests extends TestHelpers with WskTestHelpers with BeforeAndAfterAll with WskActorSystem {

  implicit val wskprops = WskProps()
  val wsk = new Wsk()
  val allowedActionDuration = 120 seconds

  // statuses for deployWeb
  val successStatus =
    """"status": "success""""

  val deployTestRepo = "https://github.com/ibm-functions/template-reminder-slack"
  val slackAction = "send-message"
  val slackSequence = "post_message_slack_sequence"
  val packageName = "myPackage"
  val ruleName = "myRule"
  val triggerName = "myTrigger"
  val bindingSlack = "openwhisk-slack"
  val slackPostAction = bindingSlack + "/" + "post"
  val deployAction = "/whisk.system/deployWeb/wskdeploy"
  val deployActionURL = s"https://${wskprops.apihost}/api/v1/web${deployAction}.http"
  val namespace = wsk.namespace.whois()
  val cron = "0 * * * *"

  //set parameters for deploy tests
  val nodejs8kind = "nodejs:8"
  val nodejs6kind = "nodejs:6"
  val phpkind = "php:7.1"
  val pythonkind = "python-jessie:3"
  val swiftkind = "swift:4.1"

  behavior of "Get Slack Reminder Template"

  // test to create the nodejs 8 reminder slack template from github url.  Will use preinstalled folder.
  it should "create the nodejs 8 reminder slack template from github url" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      // create unique asset names
      val timestamp: String = System.currentTimeMillis.toString
      val nodejs8Package = packageName + timestamp
      val nodejs8Trigger = triggerName + timestamp
      val nodejs8Rule = ruleName + timestamp
      val nodejs8SlackAction = nodejs8Package + "/" + slackAction
      val nodejs8Sequence = nodejs8Package + "/" + slackSequence
      val nodejs8RuntimePath = "runtimes/nodejs"

      // post call to deploy package to test deploy of manifest
      makePostCallWithExpectedResult(
        JsObject(
          "gitUrl" -> JsString(deployTestRepo),
          "manifestPath" -> JsString(nodejs8RuntimePath),
          "envData" -> JsObject(
            "PACKAGE_NAME" -> JsString(nodejs8Package),
            "SLACK_WEBHOOK_URL" -> JsString("https://hooks.slack.com"),
            "ALARM_CRON" -> JsString(cron),
            "TRIGGER_NAME" -> JsString(nodejs8Trigger),
            "RULE_NAME" -> JsString(nodejs8Rule)),
          "wskApiHost" -> JsString(wskprops.apihost),
          "wskAuth" -> JsString(wskprops.authKey)),
        successStatus,
        200)

      // check that both actions were created and can be invoked
      withActivation(wsk.activation, wsk.action.invoke(slackPostAction)) {
        _.response.result.get.toString should include("No text provided")
      }

      withActivation(wsk.activation, wsk.action.invoke(nodejs8SlackAction)) {
        _.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
      }

      // confirm trigger exists
      val triggers = wsk.trigger.list()
      verifyTriggerList(triggers, nodejs8Trigger)

      // confirm trigger will fire
      val triggerRun = wsk.trigger.fire(nodejs8Trigger)
      withActivation(wsk.activation, triggerRun) { activation =>
        val logEntry = activation.logs.get(0).parseJson.asJsObject
        val triggerActivationId: String = logEntry.getFields("activationId")(0).convertTo[String]
        withActivation(wsk.activation, triggerActivationId) { triggerActivation =>
          triggerActivation.response.status should include("success")
        }
      }

      // confirm rule exists
      val rules = wsk.rule.list()
      verifyRule(rules, nodejs8Rule, nodejs8Trigger, nodejs8Sequence)

      // check that sequence was created and contains correct actions
      val compValue =
        JsArray(JsString("/" + namespace + "/" + nodejs8SlackAction), JsString("/" + namespace + "/" + slackPostAction))
      val sequence = wsk.action.get(nodejs8Sequence)
      verifyActionSequence(sequence, nodejs8Sequence, compValue, JsString("sequence"))

      // verify action exists as correct kind
      val action = wsk.action.get(nodejs8SlackAction)
      verifyAction(action, nodejs8SlackAction, JsString(nodejs8kind))

      // clean up after test
      wsk.action.delete(nodejs8SlackAction)
      wsk.action.delete(nodejs8Sequence)
      wsk.pkg.delete(bindingSlack)
      wsk.pkg.delete(nodejs8Package)
      wsk.trigger.delete(nodejs8Trigger)
      wsk.rule.delete(nodejs8Rule)
  }

  // test to create the nodejs 6 reminder slack template from github url.  Will use preinstalled folder.
  it should "create the nodejs 6 reminder slack template from github url" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      // create unique asset names
      val timestamp: String = System.currentTimeMillis.toString
      val nodejs6Package = packageName + timestamp
      val nodejs6Trigger = triggerName + timestamp
      val nodejs6Rule = ruleName + timestamp
      val nodejs6SlackAction = nodejs6Package + "/" + slackAction
      val nodejs6Sequence = nodejs6Package + "/" + slackSequence
      val nodejs6RuntimePath = "runtimes/nodejs-6"

      // post call to deploy package to test deploy of manifest
      makePostCallWithExpectedResult(
        JsObject(
          "gitUrl" -> JsString(deployTestRepo),
          "manifestPath" -> JsString(nodejs6RuntimePath),
          "envData" -> JsObject(
            "PACKAGE_NAME" -> JsString(nodejs6Package),
            "SLACK_WEBHOOK_URL" -> JsString("https://hooks.slack.com"),
            "ALARM_CRON" -> JsString(cron),
            "TRIGGER_NAME" -> JsString(nodejs6Trigger),
            "RULE_NAME" -> JsString(nodejs6Rule)),
          "wskApiHost" -> JsString(wskprops.apihost),
          "wskAuth" -> JsString(wskprops.authKey)),
        successStatus,
        200)

      // check that both actions were created and can be invoked
      withActivation(wsk.activation, wsk.action.invoke(slackPostAction)) {
        _.response.result.get.toString should include("No text provided")
      }

      withActivation(wsk.activation, wsk.action.invoke(nodejs6SlackAction)) {
        _.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
      }

      // confirm trigger exists
      val triggers = wsk.trigger.list()
      verifyTriggerList(triggers, nodejs6Trigger)

      // confirm trigger will fire
      val triggerRun = wsk.trigger.fire(nodejs6Trigger)
      withActivation(wsk.activation, triggerRun) { activation =>
        val logEntry = activation.logs.get(0).parseJson.asJsObject
        val triggerActivationId: String = logEntry.getFields("activationId")(0).convertTo[String]
        withActivation(wsk.activation, triggerActivationId) { triggerActivation =>
          triggerActivation.response.status should include("success")
        }
      }

      // confirm rule exists
      val rules = wsk.rule.list()
      verifyRule(rules, nodejs6Rule, nodejs6Trigger, nodejs6Sequence)

      // check that sequence was created and contains correct actions
      val compValue =
        JsArray(JsString("/" + namespace + "/" + nodejs6SlackAction), JsString("/" + namespace + "/" + slackPostAction))
      val sequence = wsk.action.get(nodejs6Sequence)
      verifyActionSequence(sequence, nodejs6Sequence, compValue, JsString("sequence"))

      // verify action exists as correct kind
      val action = wsk.action.get(nodejs6SlackAction)
      verifyAction(action, nodejs6SlackAction, JsString(nodejs6kind))

      // clean up after test
      wsk.action.delete(nodejs6SlackAction)
      wsk.action.delete(nodejs6Sequence)
      wsk.pkg.delete(bindingSlack)
      wsk.pkg.delete(nodejs6Package)
      wsk.trigger.delete(nodejs6Trigger)
      wsk.rule.delete(nodejs6Rule)
  }

  // test to create the php reminder slack template from github url.  Will use preinstalled folder.
  it should "create the php reminder slack template from github url" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      // create unique asset names
      val timestamp: String = System.currentTimeMillis.toString
      val phpPackage = packageName + timestamp
      val phpTrigger = triggerName + timestamp
      val phpRule = ruleName + timestamp
      val phpSlackAction = phpPackage + "/" + slackAction
      val phpSequence = phpPackage + "/" + slackSequence
      val phpRuntimePath = "runtimes/php"

      // post call to deploy package to test deploy of manifest
      makePostCallWithExpectedResult(
        JsObject(
          "gitUrl" -> JsString(deployTestRepo),
          "manifestPath" -> JsString(phpRuntimePath),
          "envData" -> JsObject(
            "PACKAGE_NAME" -> JsString(phpPackage),
            "SLACK_WEBHOOK_URL" -> JsString("https://hooks.slack.com"),
            "ALARM_CRON" -> JsString(cron),
            "TRIGGER_NAME" -> JsString(phpTrigger),
            "RULE_NAME" -> JsString(phpRule)),
          "wskApiHost" -> JsString(wskprops.apihost),
          "wskAuth" -> JsString(wskprops.authKey)),
        successStatus,
        200)

      // check that both actions were created and can be invoked
      withActivation(wsk.activation, wsk.action.invoke(slackPostAction)) {
        _.response.result.get.toString should include("No text provided")
      }

      withActivation(wsk.activation, wsk.action.invoke(phpSlackAction)) {
        _.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
      }

      // confirm trigger exists
      val triggers = wsk.trigger.list()
      verifyTriggerList(triggers, phpTrigger)

      // confirm trigger will fire
      val triggerRun = wsk.trigger.fire(phpTrigger)
      withActivation(wsk.activation, triggerRun) { activation =>
        val logEntry = activation.logs.get(0).parseJson.asJsObject
        val triggerActivationId: String = logEntry.getFields("activationId")(0).convertTo[String]
        withActivation(wsk.activation, triggerActivationId) { triggerActivation =>
          triggerActivation.response.status should include("success")
        }
      }

      // confirm rule exists
      val rules = wsk.rule.list()
      verifyRule(rules, phpRule, phpTrigger, phpSequence)

      // check that sequence was created and contains correct actions
      val compValue =
        JsArray(JsString("/" + namespace + "/" + phpSlackAction), JsString("/" + namespace + "/" + slackPostAction))
      val sequence = wsk.action.get(phpSequence)
      verifyActionSequence(sequence, phpSequence, compValue, JsString("sequence"))

      // verify action exists as correct kind
      val action = wsk.action.get(phpSlackAction)
      verifyAction(action, phpSlackAction, JsString(phpkind))

      // clean up after test
      wsk.action.delete(phpSlackAction)
      wsk.action.delete(phpSequence)
      wsk.pkg.delete(bindingSlack)
      wsk.pkg.delete(phpPackage)
      wsk.trigger.delete(phpTrigger)
      wsk.rule.delete(phpRule)
  }

  // test to create the python reminder slack template from github url.  Will use preinstalled folder.
  it should "create the python reminder slack template from github url" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      // create unique asset names
      val timestamp: String = System.currentTimeMillis.toString
      val pythonPackage = packageName + timestamp
      val pythonTrigger = triggerName + timestamp
      val pythonRule = ruleName + timestamp
      val pythonSlackAction = pythonPackage + "/" + slackAction
      val pythonSequence = pythonPackage + "/" + slackSequence
      val pythonRuntimePath = "runtimes/python"

      // post call to deploy package to test deploy of manifest
      makePostCallWithExpectedResult(
        JsObject(
          "gitUrl" -> JsString(deployTestRepo),
          "manifestPath" -> JsString(pythonRuntimePath),
          "envData" -> JsObject(
            "PACKAGE_NAME" -> JsString(pythonPackage),
            "SLACK_WEBHOOK_URL" -> JsString("https://hooks.slack.com"),
            "ALARM_CRON" -> JsString(cron),
            "TRIGGER_NAME" -> JsString(pythonTrigger),
            "RULE_NAME" -> JsString(pythonRule)),
          "wskApiHost" -> JsString(wskprops.apihost),
          "wskAuth" -> JsString(wskprops.authKey)),
        successStatus,
        200)

      // check that both actions were created and can be invoked
      withActivation(wsk.activation, wsk.action.invoke(slackPostAction)) {
        _.response.result.get.toString should include("No text provided")
      }

      withActivation(wsk.activation, wsk.action.invoke(pythonSlackAction)) {
        _.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
      }

      // confirm trigger exists
      val triggers = wsk.trigger.list()
      verifyTriggerList(triggers, pythonTrigger)

      // confirm trigger will fire
      val triggerRun = wsk.trigger.fire(pythonTrigger)
      withActivation(wsk.activation, triggerRun) { activation =>
        val logEntry = activation.logs.get(0).parseJson.asJsObject
        val triggerActivationId: String = logEntry.getFields("activationId")(0).convertTo[String]
        withActivation(wsk.activation, triggerActivationId) { triggerActivation =>
          triggerActivation.response.status should include("success")
        }
      }

      // confirm rule exists
      val rules = wsk.rule.list()
      verifyRule(rules, pythonRule, pythonTrigger, pythonSequence)

      // check that sequence was created and contains correct actions
      val compValue =
        JsArray(JsString("/" + namespace + "/" + pythonSlackAction), JsString("/" + namespace + "/" + slackPostAction))
      val sequence = wsk.action.get(pythonSequence)
      verifyActionSequence(sequence, pythonSequence, compValue, JsString("sequence"))

      // verify action exists as correct kind
      val action = wsk.action.get(pythonSlackAction)
      verifyAction(action, pythonSlackAction, JsString(pythonkind))

      // clean up after test
      wsk.action.delete(pythonSlackAction)
      wsk.action.delete(pythonSequence)
      wsk.pkg.delete(bindingSlack)
      wsk.pkg.delete(pythonPackage)
      wsk.trigger.delete(pythonTrigger)
      wsk.rule.delete(pythonRule)
  }

  // test to create the swift reminder slack template from github url.  Will use preinstalled folder.
  it should "create the swift reminder slack template from github url" in withAssetCleaner(wskprops) {
    (wp, assetHelper) =>
      // create unique asset names
      val timestamp: String = System.currentTimeMillis.toString
      val swiftPackage = packageName + timestamp
      val swiftTrigger = triggerName + timestamp
      val swiftRule = ruleName + timestamp
      val swiftSlackAction = swiftPackage + "/" + slackAction
      val swiftSequence = swiftPackage + "/" + slackSequence
      val swiftRuntimePath = "runtimes/swift"

      // post call to deploy package to test deploy of manifest
      makePostCallWithExpectedResult(
        JsObject(
          "gitUrl" -> JsString(deployTestRepo),
          "manifestPath" -> JsString(swiftRuntimePath),
          "envData" -> JsObject(
            "PACKAGE_NAME" -> JsString(swiftPackage),
            "SLACK_WEBHOOK_URL" -> JsString("https://hooks.slack.com"),
            "ALARM_CRON" -> JsString(cron),
            "TRIGGER_NAME" -> JsString(swiftTrigger),
            "RULE_NAME" -> JsString(swiftRule)),
          "wskApiHost" -> JsString(wskprops.apihost),
          "wskAuth" -> JsString(wskprops.authKey)),
        successStatus,
        200)

      // check that both actions were created and can be invoked
      withActivation(wsk.activation, wsk.action.invoke(slackPostAction)) {
        _.response.result.get.toString should include("No text provided")
      }

      withActivation(wsk.activation, wsk.action.invoke(swiftSlackAction)) {
        _.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
      }

      // confirm trigger exists
      val triggers = wsk.trigger.list()
      verifyTriggerList(triggers, swiftTrigger)

      // confirm trigger will fire
      val triggerRun = wsk.trigger.fire(swiftTrigger)
      withActivation(wsk.activation, triggerRun) { activation =>
        val logEntry = activation.logs.get(0).parseJson.asJsObject
        val triggerActivationId: String = logEntry.getFields("activationId")(0).convertTo[String]
        withActivation(wsk.activation, triggerActivationId) { triggerActivation =>
          triggerActivation.response.status should include("success")
        }
      }

      // confirm rule exists
      val rules = wsk.rule.list()
      verifyRule(rules, swiftRule, swiftTrigger, swiftSequence)

      // check that sequence was created and contains correct actions
      val compValue =
        JsArray(JsString("/" + namespace + "/" + swiftSlackAction), JsString("/" + namespace + "/" + slackPostAction))
      val sequence = wsk.action.get(swiftSequence)
      verifyActionSequence(sequence, swiftSequence, compValue, JsString("sequence"))

      // verify action exists as correct kind
      val action = wsk.action.get(swiftSlackAction)
      verifyAction(action, swiftSlackAction, JsString(swiftkind))

      // clean up after test
      wsk.action.delete(swiftSlackAction)
      wsk.action.delete(swiftSequence)
      wsk.pkg.delete(bindingSlack)
      wsk.pkg.delete(swiftPackage)
      wsk.trigger.delete(swiftTrigger)
      wsk.rule.delete(swiftRule)
  }

  /**
   * Test the nodejs 6 "Get Slack Reminder Template" template
   */
  it should "invoke nodejs 6 send-message.js and get the result" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val nodejs6folder = "../runtimes/nodejs-6/actions"
    val timestamp: String = System.currentTimeMillis.toString
    val name = "messageNode6" + timestamp
    val file = Some(new File(nodejs6folder, "send-message.js").toString())
    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, file, kind = Some(nodejs6kind))
    }

    withActivation(wsk.activation, wsk.action.invoke(name)) { activation =>
      activation.response.success shouldBe true
      activation.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
    }
  }

  /**
   * Test the nodejs 8 "Get Slack Reminder Template" template
   */
  it should "invoke nodejs 8 send-message.js and get the result" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val nodejs8folder = "../runtimes/nodejs/actions"
    val timestamp: String = System.currentTimeMillis.toString
    val name = "messageNode8" + timestamp
    val file = Some(new File(nodejs8folder, "send-message.js").toString())

    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, file, kind = Some(nodejs8kind))
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
    val phpfolder = "../runtimes/php/actions"
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
    val pythonfolder = "../runtimes/python/actions"
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
    val swiftfolder = "../runtimes/swift/actions"
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

  private def verifyRule(ruleListResult: RunResult, ruleName: String, triggerName: String, actionName: String) = {
    val actionNameWithNoPackage = actionName.split("/").last
    val ruleAsObject = wsk.parseJsonString(wsk.rule.get(ruleName).stdout)
    ruleAsObject.fields("name") shouldBe JsString(ruleName)
    ruleAsObject.fields("trigger").asJsObject.fields("name") shouldBe JsString(triggerName)
    ruleAsObject.fields("action").asJsObject.fields("name") shouldBe JsString(actionNameWithNoPackage)
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

  def verifyActionSequence(action: RunResult, name: String, compValue: JsArray, kindValue: JsString): Unit = {
    val stdout = action.stdout
    assert(stdout.startsWith(s"ok: got action $name\n"))
    wsk.parseJsonString(stdout).fields("exec").asJsObject.fields("components") shouldBe compValue
    wsk.parseJsonString(stdout).fields("exec").asJsObject.fields("kind") shouldBe kindValue
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
}
