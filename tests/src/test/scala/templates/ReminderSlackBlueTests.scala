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
import common.TestUtils.RunResult
import common.ActivationResult
import common.{TestHelpers, Wsk, WskProps, WskTestHelpers}
import java.io._
import com.jayway.restassured.RestAssured
import com.jayway.restassured.config.SSLConfig
import spray.json.DefaultJsonProtocol._
import spray.json._
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class ReminderSlackBlueTests extends TestHelpers
  with WskTestHelpers
  with BeforeAndAfterAll {

  implicit val wskprops = WskProps()
  val wsk = new Wsk()
  val allowedActionDuration = 120 seconds

  //set parameters for deploy tests
  val nodejs8folder = "../runtimes/nodejs/actions"
  val nodejs8kind = "nodejs:8"
  val nodejs6folder = "../runtimes/nodejs-6/actions"
  val nodejs6kind = "nodejs:6"
  val phpfolder = "../runtimes/php/actions"
  val phpkind = "php:7.1"
  val pythonfolder = "../runtimes/python/actions"
  val pythonkind = "python-jessie:3"
  val swiftfolder = "../runtimes/swift/actions"
  val swiftkind = "swift:4.1"

  behavior of "Get Slack Reminder Template"

  /**
    * Test the nodejs 6 "Get Slack Reminder Template" template
    */
  it should "invoke nodejs 6 send-message.js and get the result" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val timestamp: String = System.currentTimeMillis.toString
    val name = "messageNode6" + timestamp
    val file = Some(new File(nodejs6folder, "send-message.js").toString())
    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, file, kind = Some(nodejs6kind))
    }

    withActivation(wsk.activation, wsk.action.invoke(name)) {
      activation =>
        activation.response.success shouldBe true
        activation.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
    }
  }
  /**
    * Test the nodejs 8 "Get Slack Reminder Template" template
    */
  it should "invoke nodejs 8 send-message.js and get the result" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
    val timestamp: String = System.currentTimeMillis.toString
    val name = "messageNode8" + timestamp
    val file = Some(new File(nodejs8folder, "send-message.js").toString())

    assetHelper.withCleaner(wsk.action, name) { (action, _) =>
      action.create(name, file, kind = Some(nodejs8kind))
    }

    withActivation(wsk.activation, wsk.action.invoke(name)) {
      activation =>
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

    withActivation(wsk.activation, wsk.action.invoke(name)) {
      activation =>
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

    withActivation(wsk.activation, wsk.action.invoke(name)) {
      activation =>
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
    withActivation(wsk.activation, wsk.action.invoke(name)) {
      activation =>
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
    val response = RestAssured.given()
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
