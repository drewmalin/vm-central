package com.drewmalin.vm.central.data.repository;

import com.drewmalin.vm.central.data.model.UserDTO;
import com.drewmalin.vm.central.data.model.VmDTO;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.SqlTemplate;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class VmRepositorySql
    implements VmRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(VmRepositorySql.class);

    private final Pool sqlPool;

    public VmRepositorySql(final Pool sqlPool) {
        this.sqlPool = sqlPool;
    }

    @Override
    public Future<List<VmDTO>> getAll() {
        final var query = """
            SELECT
                vm.virtual_machine_pk,
                vm.virtual_machine_id,
                vm.provider,
                vm.status,
                u.user_pk,
                u.user_id,
                u.username,
                u.first_name,
                u.last_name,
                u.hashed_password,
                u.salt,
                u.role_id
            FROM virtual_machines AS vm
            JOIN users as u ON u.user_pk = vm.user_fk
            """;

        final RowMapper<VmDTO> mapper = row -> {
            final var user = UserDTO.builder()
                .pk(row.getInteger("user_pk"))
                .id(row.getString("user_id"))
                .username(row.getString("username"))
                .hashedPassword(row.getBuffer("hashed_password").getBytes())
                .salt(row.getBuffer("salt").getBytes())
                .firstName(row.getString("first_name"))
                .lastName(row.getString("last_name"))
                .roleId(row.getString("role_id"))
                .build();

            return VmDTO.builder()
                .pk(row.getInteger("virtual_machine_pk"))
                .id(row.getString("virtual_machine_id"))
                .provider(row.getString("provider"))
                .status(row.getString("status"))
                .owner(user)
                .build();
        };

        return this.sqlPool.withConnection(sqlConnection -> SqlTemplate.forQuery(sqlConnection, query)
            .mapTo(mapper)
            .execute(Collections.emptyMap())
            .map(rowSet -> {
                final List<VmDTO> vms = new ArrayList<>();
                for (final VmDTO vm : rowSet) {
                    vms.add(vm);
                }
                return vms;
            }));
    }

    @Override
    public Future<VmDTO> add(final VmDTO vm, final boolean ensureUnique) {
        final var query = """
            INSERT INTO virtual_machines (
                virtual_machine_id,
                provider,
                status,
                user_fk
            )
            VALUES (
                #{virtual_machine_id},
                #{provider},
                #{status},
                #{user_pk}
            )
            RETURNING
                virtual_machine_pk,
                virtual_machine_id,
                provider,
                status,
                user_fk
            """;

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("virtual_machine_id", vm.id());
        parameters.put("provider", vm.providerName());
        parameters.put("status", vm.statusName());
        parameters.put("user_pk", vm.owner().pk());

        return this.sqlPool.withConnection(sqlConnection -> SqlTemplate.forUpdate(sqlConnection, query)
            .mapTo(VmDTO.class)
            .execute(parameters)
            .map(rowSet -> {
                final RowIterator<VmDTO> iterator = rowSet.iterator();
                if (iterator.hasNext()) {
                    final var responseVm = iterator.next();

                    return VmDTO.builder(responseVm)
                        .pk(responseVm.pk())
                        .id(responseVm.id())
                        .provider(responseVm.providerName())
                        .status(responseVm.statusName())
                        .owner(vm.owner())
                        .build();
                }
                else {
                    throw new IllegalArgumentException("");
                }
            }));
    }

    @Override
    public Future<VmDTO> put(final VmDTO vm) {
        final var query = """
            UPDATE virtual_machines
            SET
                provider = #{provider},
                status = #{status},
                user_fk = #{user_pk}
            WHERE
                virtual_machine_pk = #{pk}
            RETURNING
                virtual_machine_pk,
                virtual_machine_id,
                provider,
                status,
                user_fk
            """;

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("pk", vm.pk());
        parameters.put("provider", vm.providerName());
        parameters.put("status", vm.statusName());
        parameters.put("user_pk", vm.owner().pk());

        return this.sqlPool.withConnection(sqlConnection -> SqlTemplate.forUpdate(sqlConnection, query)
            .mapTo(VmDTO.class)
            .execute(parameters)
            .map(rowSet -> {
                final RowIterator<VmDTO> iterator = rowSet.iterator();
                if (iterator.hasNext()) {
                    final var responseVm = iterator.next();

                    return VmDTO.builder()
                        .pk(responseVm.pk())
                        .id(responseVm.id())
                        .provider(responseVm.vmProvider())
                        .status(responseVm.vmStatus())
                        .owner(vm.owner())
                        .build();
                }
                else {
                    throw new IllegalArgumentException("");
                }
            }));
    }

    @Override
    public Future<List<VmDTO>> putAll(final List<VmDTO> vms) {
        final var query = """
            UPDATE virtual_machines
            SET
                provider = #{provider},
                status = #{status},
                user_fk = #{user_pk}
            WHERE
                virtual_machine_pk = #{pk}
            RETURNING
                virtual_machine_pk,
                virtual_machine_id,
                provider,
                status,
                user_fk
            """;

        // Save the users for later instead of needlessly re-fetching them from the database
        final Map<Integer, UserDTO> vmPkToUserMap = new HashMap<>();

        final List<Map<String, Object>> parameterList = new ArrayList<>();
        for (final var vm : vms) {

            vmPkToUserMap.put(vm.pk(), vm.owner());

            final var parameters = new HashMap<String, Object>();

            parameters.put("pk", vm.pk());
            parameters.put("provider", vm.providerName());
            parameters.put("status", vm.statusName());
            parameters.put("user_pk", vm.owner().pk());

            parameterList.add(parameters);
        }

        return this.sqlPool.withConnection(sqlConnection -> SqlTemplate.forUpdate(sqlConnection, query)
            .mapTo(VmDTO.class)
            .executeBatch(parameterList)
            .map(rowSet -> {
                final List<VmDTO> resultVms = new ArrayList<>();

                for (final VmDTO responseVm : rowSet) {
                    resultVms.add(
                        VmDTO.builder()
                            .pk(responseVm.pk())
                            .id(responseVm.id())
                            .provider(responseVm.vmProvider())
                            .status(responseVm.vmStatus())
                            .owner(vmPkToUserMap.get(responseVm.pk()))
                            .build()
                    );
                }

                return resultVms;
            }));
    }

    @Override
    public Future<VmDTO> get(final String id) {
        final var query = """
            SELECT
                vm.virtual_machine_pk,
                vm.virtual_machine_id,
                vm.provider,
                vm.status,
                u.user_pk,
                u.user_id,
                u.username,
                u.first_name,
                u.last_name,
                u.hashed_password,
                u.salt,
                u.role_id
            FROM virtual_machines AS vm
            JOIN users as u ON u.user_pk = vm.user_fk
            WHERE vm.virtual_machine_id = #{id}
            """;

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);

        final RowMapper<VmDTO> mapper = row -> {
            final var user = UserDTO.builder()
                .pk(row.getInteger("user_pk"))
                .id(row.getString("user_id"))
                .username(row.getString("username"))
                .hashedPassword(row.getBuffer("hashed_password").getBytes())
                .salt(row.getBuffer("salt").getBytes())
                .firstName(row.getString("first_name"))
                .lastName(row.getString("last_name"))
                .roleId(row.getString("role_id"))
                .build();

            return VmDTO.builder()
                .pk(row.getInteger("virtual_machine_pk"))
                .id(row.getString("virtual_machine_id"))
                .provider(row.getString("provider"))
                .status(row.getString("status"))
                .owner(user)
                .build();
        };

        return this.sqlPool.withConnection(sqlConnection -> SqlTemplate.forQuery(sqlConnection, query)
            .mapTo(mapper)
            .execute(parameters)
            .map(rowSet -> {
                final RowIterator<VmDTO> iterator = rowSet.iterator();
                if (iterator.hasNext()) {
                    return iterator.next();
                }
                else {
                    throw new NoSuchElementException();
                }
            }));
    }

    @Override
    public Future<Void> delete(final String id) {
        throw new NotImplementedException();
    }
}
