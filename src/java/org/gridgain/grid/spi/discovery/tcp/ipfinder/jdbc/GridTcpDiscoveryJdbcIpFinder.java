// Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.spi.discovery.tcp.ipfinder.jdbc;

import org.gridgain.grid.editions.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.typedef.*;
import org.gridgain.grid.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;

import javax.sql.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static java.sql.Connection.*;

/**
 * JDBC-based IP finder.
 * <h1 class="header">Configuration</h1>
 * <h2 class="header">Mandatory</h2>
 * <ul>
 *     <li>Data source (see {@link #setDataSource(DataSource)}).</li>
 * </ul>
 * <h2 class="header">Optional</h2>
 * There are no optional configuration parameters.
 * <p>
 * The database will contain 1 table which will hold IP addresses.
 *
 * @author 2012 Copyright (C) GridGain Systems
 * @version 4.0.0c.22032012
 */
public class GridTcpDiscoveryJdbcIpFinder extends GridTcpDiscoveryIpFinderAdapter {
    /** Query to get addresses. */
    public static final String GET_ADDRS_QRY = "select hostname, port from tbl_addrs";

    /** Query to register address. */
    public static final String REG_ADDR_QRY = "insert into tbl_addrs values (?, ?)";

    /** Query to unregister address. */
    public static final String UNREG_ADDR_QRY = "delete from tbl_addrs where hostname = ? and port = ?";

    /** Query to create addresses table. */
    public static final String CREATE_ADDRS_TABLE_QRY =
        "create table if not exists tbl_addrs (" +
        "hostname VARCHAR(1024), " +
        "port INT)";

    /** Query to check database validity. */
    public static final String CHK_QRY = "select count(*) from tbl_addrs";

    /** Grid logger. */
    @GridLoggerResource
    private GridLogger log;

    /** Data source. */
    private DataSource dataSrc;

    /** Init guard. */
    @GridToStringExclude
    private final AtomicBoolean initGuard = new AtomicBoolean();

    /** Init latch. */
    @GridToStringExclude
    private final CountDownLatch initLatch = new CountDownLatch(1);

    /**
     * Constructor.
     */
    public GridTcpDiscoveryJdbcIpFinder() {
        setShared(true);
    }

    /** {@inheritDoc} */
    @Override public Collection<InetSocketAddress> getRegisteredAddresses() throws GridSpiException {
        init();

        Connection conn = null;

        PreparedStatement stmt = null;

        ResultSet rs = null;

        try {
            conn = dataSrc.getConnection();

            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);

            stmt = conn.prepareStatement(GET_ADDRS_QRY);

            rs = stmt.executeQuery();

            Collection<InetSocketAddress> addrs = new LinkedList<InetSocketAddress>();

            while (rs.next())
                addrs.add(new InetSocketAddress(rs.getString(1), rs.getInt(2)));

            return addrs;
        }
        catch (SQLException e) {
            throw new GridSpiException("Failed to get registered addresses version.", e);
        }
        finally {
            U.closeQuiet(rs);
            U.closeQuiet(stmt);
            U.closeQuiet(conn);
        }
    }

    /** {@inheritDoc} */
    @Override public void registerAddresses(Collection<InetSocketAddress> addrs) throws GridSpiException {
        assert !F.isEmpty(addrs);

        init();

        Connection conn = null;

        PreparedStatement stmtUnreg = null;

        PreparedStatement stmtReg = null;

        boolean committed = false;

        try {
            conn = dataSrc.getConnection();

            conn.setAutoCommit(false);

            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);

            stmtUnreg = conn.prepareStatement(UNREG_ADDR_QRY);
            stmtReg = conn.prepareStatement(REG_ADDR_QRY);

            for (InetSocketAddress addr : addrs) {
                stmtUnreg.setString(1, addr.getAddress().getHostAddress());
                stmtUnreg.setInt(2, addr.getPort());

                stmtUnreg.addBatch();

                stmtReg.setString(1, addr.getAddress().getHostAddress());
                stmtReg.setInt(2, addr.getPort());

                stmtReg.addBatch();
            }

            stmtUnreg.executeBatch();
            stmtUnreg.close();

            stmtReg.executeBatch();
            stmtReg.close();

            conn.commit();

            committed = true;
        }
        catch (SQLException e) {
            U.rollbackConnectionQuiet(conn);

            throw new GridSpiException("Failed to register addresses: " + addrs, e);
        }
        finally {
            if (!committed)
                U.rollbackConnectionQuiet(conn);

            U.closeQuiet(stmtUnreg);
            U.closeQuiet(stmtReg);
            U.closeQuiet(conn);
        }
    }

    /** {@inheritDoc} */
    @Override public void unregisterAddresses(Collection<InetSocketAddress> addrs) throws GridSpiException {
        assert !F.isEmpty(addrs);

        init();

        Connection conn = null;

        PreparedStatement stmt = null;

        boolean committed = false;

        try {
            conn = dataSrc.getConnection();

            conn.setAutoCommit(false);

            conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);

            stmt = conn.prepareStatement(UNREG_ADDR_QRY);

            for (InetSocketAddress addr : addrs) {
                stmt.setString(1, addr.getAddress().getHostAddress());
                stmt.setInt(2, addr.getPort());

                stmt.addBatch();
            }

            stmt.executeBatch();
            conn.commit();

            committed = true;
        }
        catch (SQLException e) {
            U.rollbackConnectionQuiet(conn);

            throw new GridSpiException("Failed to unregister addresses: " + addrs, e);
        }
        finally {
            if (!committed)
                U.rollbackConnectionQuiet(conn);

            U.closeQuiet(stmt);
            U.closeQuiet(conn);
        }
    }

    /**
     * Sets data source.
     * <p>
     * Data source should be fully configured and ready-to-use.
     *
     * @param dataSrc Data source.
     */
    @GridSpiConfiguration(optional = false)
    public void setDataSource(DataSource dataSrc) {
        this.dataSrc = dataSrc;
    }

    /**
     * Checks configuration validity.
     *
     * @throws GridSpiException If any error occurs.
     */
    private void init() throws GridSpiException {
        if (initGuard.compareAndSet(false, true)) {
            if (dataSrc == null)
                throw new GridSpiException("Data source is null (you must configure it via setDataSource(..)" +
                    " configuration property)");

            Connection conn = null;

            Statement stmt = null;

            boolean committed = false;

            try {
                conn = dataSrc.getConnection();

                conn.setAutoCommit(false);

                conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);

                // Create tbl_addrs.
                stmt = conn.createStatement();

                stmt.executeUpdate(CREATE_ADDRS_TABLE_QRY);

                conn.commit();

                committed = true;

                if (log.isDebugEnabled())
                    log.debug("DB schema has been initialized.");
            }
            catch (SQLException e) {
                U.rollbackConnectionQuiet(conn);

                throw new GridSpiException("Failed to initialize DB schema.", e);
            }
            finally {
                if (!committed)
                    U.rollbackConnectionQuiet(conn);

                U.closeQuiet(stmt);
                U.closeQuiet(conn);

                initLatch.countDown();
            }
        }
        else {
            try {
                initLatch.await();
            }
            catch (InterruptedException e) {
                throw new GridSpiException("Thread has been interrupted.", e);
            }

            Connection conn = null;

            Statement stmt = null;

            try {
                conn = dataSrc.getConnection();

                conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED);

                // Check if tbl_addrs exists and database initialized properly.
                stmt = conn.createStatement();

                stmt.execute(CHK_QRY);
            }
            catch (SQLException e) {
                throw new GridSpiException("IP finder has not been properly initialized.", e);
            }
            finally {
                U.closeQuiet(stmt);
                U.closeQuiet(conn);
            }
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridTcpDiscoveryJdbcIpFinder.class, this);
    }
}
