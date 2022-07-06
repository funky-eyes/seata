package io.seata.rm.datasource.exec;
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
import java.sql.SQLException;

/**
 * @author jianbin.chen
 */
public class TxRetryException extends SQLException {

	public TxRetryException(String reason, String SQLState, int vendorCode) {
		super(reason, SQLState, vendorCode);
	}

	public TxRetryException(String reason, String SQLState) {
		super(reason, SQLState);
	}

	public TxRetryException(String reason) {
		super(reason);
	}

	public TxRetryException() {
	}

	public TxRetryException(Throwable cause) {
		super(cause);
	}

	public TxRetryException(String reason, Throwable cause) {
		super(reason, cause);
	}

	public TxRetryException(String reason, String sqlState, Throwable cause) {
		super(reason, sqlState, cause);
	}

	public TxRetryException(String reason, String sqlState, int vendorCode, Throwable cause) {
		super(reason, sqlState, vendorCode, cause);
	}
}
