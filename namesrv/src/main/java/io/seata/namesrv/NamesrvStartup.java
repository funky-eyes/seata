/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.namesrv;

import io.seata.namesrv.raft.RaftNamesrvController;

/**
 * @author funkye
 */
public class NamesrvStartup {

    public static void start(String[] args){
        try {
            NamesrvController controller = createNamesrvController(args);
            controller.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static NamesrvController createNamesrvController(String[] args) throws Exception {
        return new RaftNamesrvController();
    }

}
