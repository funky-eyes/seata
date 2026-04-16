--
-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- User: conghuhu
-- Date: 2022/7/7

-- param description
-- KEYS[1] xidLockKey
-- KEYS[2] status
-- ARGV[1] code

-- init data
local xidLockKey = KEYS[1];
local status = KEYS[2];
local code = ARGV[1];
local rowLockKeyPrefix = 'SEATA_ROW_LOCK_'
local nextRowLockKeyBoundary = ';' .. rowLockKeyPrefix

-- get table length
local function table_len(t)
    local len = 0
    for _, _ in pairs(t) do
        len = len + 1
    end
    return len;
end

local function split_row_lock_keys(target)
    local str = tostring(target)
    local strB, arrayIndex = 1, 1
    local targetArray = {}
    while (strB <= string.len(str))
    do
        local si, _ = string.find(str, nextRowLockKeyBoundary, strB, true)
        if (si)
        then
            targetArray[arrayIndex] = string.sub(str, strB, si - 1)
            arrayIndex = arrayIndex + 1
            strB = si + 1
        else
            targetArray[arrayIndex] = string.sub(str, strB, string.len(str))
            break
        end
    end
    return targetArray
end

local branchAndLockKeys = redis.call('HGETALL', xidLockKey)
for i = 1, table_len(branchAndLockKeys) do
    if (i % 2 == 0)
    then
        local k = tostring(branchAndLockKeys[i])
        local start, _ = string.find(k, nextRowLockKeyBoundary, 1, true)
        if (start)
        -- k contains multiple stored row lock keys
        then
            local keys = split_row_lock_keys(k)
            for _, key in pairs(keys)
            do
                redis.call('HSET', key, status, code)
            end
        else
            redis.call('HSET', k, status, code)
        end
    end
end
