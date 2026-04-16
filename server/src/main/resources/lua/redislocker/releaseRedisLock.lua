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
-- KEYS[2] branchId

-- init data
local xidLockKey = KEYS[1];
local branchId = KEYS[2];

local rowKeys = {}
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

-- split on row lock key boundaries instead of plain ';' so resourceId semicolons are preserved
local function split_row_lock_keys(target)
    local str = tostring(target)
    local strLen = string.len(str)
    local strB, arrayIndex = 1, 1
    local targetArray = {}
    while (strB <= strLen)
    do
        local si, _ = string.find(str, nextRowLockKeyBoundary, strB, true)
        if (si)
        then
            targetArray[arrayIndex] = string.sub(str, strB, si - 1)
            arrayIndex = arrayIndex + 1
            strB = si + 1
        else
            targetArray[arrayIndex] = string.sub(str, strB, strLen)
            break
        end
    end
    return targetArray
end

-- start
if (not branchId)
then
    local rowKeyMap = redis.call('HGETALL', xidLockKey)
    for i = 1, table_len(rowKeyMap) do
        if (i % 2 == 0)
        then
            rowKeys[i / 2] = rowKeyMap[i]
        end
    end
else
    local rowKey = redis.call('HGET', xidLockKey, branchId)
    rowKeys[1] = rowKey
end

if (table_len(rowKeys) == 0)
-- rowKeys is empty
then
    return true
end

if (not branchId)
-- branchId is null
then
    redis.call('DEL', xidLockKey)
else
    redis.call('HDEL', xidLockKey, branchId)
end

for _, value in pairs(rowKeys) do
    local rowKeyStr = tostring(value)
    if (string.len(rowKeyStr) == 0)
    -- rowKeyStr is empty
    then
        return true
    end

    local start, _ = string.find(rowKeyStr, nextRowLockKeyBoundary, 1, true)
    if (start)
    -- rowKeyStr contains multiple stored row lock keys
    then
        local keys = split_row_lock_keys(rowKeyStr)
        redis.call('DEL', unpack(keys))
    else
        redis.call('DEL', rowKeyStr)
    end
end

return true


