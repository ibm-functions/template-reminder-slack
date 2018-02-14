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
import common.{TestHelpers, Wsk, WskProps, WskTestHelpers}
import java.io._
// import spray.json.DefaultJsonProtocol.StringJsonFormat
// import spray.json.pimpAny

@RunWith(classOf[JUnitRunner])
class ReminderSlackTests extends TestHelpers
    with WskTestHelpers
    with BeforeAndAfterAll {

    implicit val wskprops = WskProps()
    val wsk = new Wsk()

    //set parameters for deploy tests
    val nodejs6folder = "../runtimes/nodejs-6/actions";
    val nodejs8folder = "../runtimes/nodejs-8/actions";
    val phpfolder = "../runtimes/php-7.1/actions";
    val pythonfolder = "../runtimes/python-3/actions";
    val swiftfolder = "../runtimes/swift-3.1.1/actions";

    behavior of "Get Slack Reminder Template"

    /**
     * Test the nodejs 6 "Get Slack Reminder Template" template
     */
     it should "invoke nodejs 6 send-message.js and get the result" in withAssetCleaner(wskprops) { (wp, assetHelper) =>
       val name = "messageNode"
       val file = Some(new File(nodejs6folder, "send-message.js").toString());
       assetHelper.withCleaner(wsk.action, name) { (action, _) =>
         action.create(name, file)
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
        val name = "messageNode"
        val file = Some(new File(nodejs8folder, "send-message.js").toString());
        assetHelper.withCleaner(wsk.action, name) { (action, _) =>
          action.create(name, file)
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
        val name = "messagePhp"
        val file = Some(new File(phpfolder, "send-message.php").toString());
        assetHelper.withCleaner(wsk.action, name) { (action, _) =>
          action.create(name, file)
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
         val name = "messagePython"
         val file = Some(new File(pythonfolder, "send-message.py").toString());
         assetHelper.withCleaner(wsk.action, name) { (action, _) =>
           action.create(name, file)
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
          val name = "messageSwift"
          val file = Some(new File(swiftfolder, "send-message.swift").toString());
          assetHelper.withCleaner(wsk.action, name) { (action, _) =>
            action.create(name, file)
          }

          withActivation(wsk.activation, wsk.action.invoke(name)) {
            activation =>
             activation.response.success shouldBe true
             activation.response.result.get.toString should include("Your scrum is starting now.  Time to find your team!")
          }
        }
}
