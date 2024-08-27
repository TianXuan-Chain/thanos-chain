/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */
package com.thanos.chain.contract.eth.evm.program.invoke;



import com.thanos.chain.contract.eth.evm.DataWord;
import com.thanos.chain.contract.eth.evm.program.Program;
import com.thanos.chain.storage.db.Repository;
import com.thanos.chain.ledger.model.Block;
import com.thanos.chain.ledger.model.eth.EthTransaction;

import java.math.BigInteger;

/**
 * @author Roman Mandeleil
 * @since 19.12.2014
 */
public interface ProgramInvokeFactory {

    ProgramInvoke createProgramInvoke(EthTransaction tx, Block block,
                                      Repository repository);

    ProgramInvoke createProgramInvoke(Program program, DataWord toAddress, DataWord callerAddress,
                                      DataWord inValue, DataWord inGas,
                                      BigInteger balanceInt, byte[] dataIn,
                                      Repository repository,
                                      boolean staticCall, boolean byTestingSuite);


}
