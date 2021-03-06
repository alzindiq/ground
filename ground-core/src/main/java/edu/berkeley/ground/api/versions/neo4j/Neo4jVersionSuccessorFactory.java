/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.berkeley.ground.api.versions.neo4j;

import edu.berkeley.ground.api.versions.GroundType;
import edu.berkeley.ground.api.versions.Version;
import edu.berkeley.ground.api.versions.VersionSuccessor;
import edu.berkeley.ground.api.versions.VersionSuccessorFactory;
import edu.berkeley.ground.db.DBClient;
import edu.berkeley.ground.db.DBClient.GroundDBConnection;
import edu.berkeley.ground.db.DbDataContainer;
import edu.berkeley.ground.db.Neo4jClient;
import edu.berkeley.ground.db.Neo4jClient.Neo4jConnection;
import edu.berkeley.ground.exceptions.EmptyResultException;
import edu.berkeley.ground.exceptions.GroundException;
import edu.berkeley.ground.util.IdGenerator;
import org.neo4j.driver.internal.value.StringValue;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.types.Relationship;

import java.util.ArrayList;
import java.util.List;

public class Neo4jVersionSuccessorFactory extends VersionSuccessorFactory {
    public <T extends Version> VersionSuccessor<T> create(GroundDBConnection connectionPointer, String fromId, String toId) throws GroundException {
        Neo4jConnection connection = (Neo4jConnection) connectionPointer;
        // check if both IDs exist
        List<DbDataContainer> predicates = new ArrayList<>();
        predicates.add(new DbDataContainer("id", GroundType.STRING, fromId));
        Record record;

        try {
            record = connection.getVertex(predicates);
        } catch (EmptyResultException eer) {
            throw new GroundException("Id " + fromId + " is not valid.");
        }

        if (record == null) {
            throw new GroundException("Id " + fromId + " is not valid.");
        }

        predicates.clear();
        predicates.add(new DbDataContainer("id", GroundType.STRING, toId));
        try {
            record = connection.getVertex(predicates);
        } catch (EmptyResultException eer) {
            throw new GroundException("Id " + toId + " is not valid.");
        }

        if (record == null) {
            throw new GroundException("Id " + fromId + " is not valid.");
        }

        String dbId = IdGenerator.generateId(fromId + toId);

        predicates.clear();
        predicates.add(new DbDataContainer("id", GroundType.STRING, dbId));
        predicates.add(new DbDataContainer("fromId", GroundType.STRING, fromId));
        predicates.add(new DbDataContainer("toId", GroundType.STRING, toId));

        connection.addEdge("VersionSuccessor", fromId, toId, predicates);

        return VersionSuccessorFactory.construct(dbId, fromId, toId);
    }


    public <T extends Version> VersionSuccessor<T> retrieveFromDatabase(GroundDBConnection connectionPointer, String dbId) throws GroundException {
        Neo4jConnection connection = (Neo4jConnection) connectionPointer;
        List<DbDataContainer> predicates = new ArrayList<>();
        predicates.add(new DbDataContainer("id", GroundType.STRING, dbId));

        Relationship result;
        try {
            result = connection.getEdge("VersionSuccessor", predicates);
        } catch (EmptyResultException eer) {
            throw new GroundException("No VersionSuccessor found with id " + dbId + ".");
        }

        return this.constructFromRelationship(result);
    }

    protected <T extends Version> VersionSuccessor<T> constructFromRelationship(Relationship r) {
        String id = Neo4jClient.getStringFromValue((StringValue) r.get("id"));
        String fromId = Neo4jClient.getStringFromValue((StringValue) r.get("fromId"));
        String toId = Neo4jClient.getStringFromValue((StringValue) r.get("toId"));

        return VersionSuccessorFactory.construct(id, fromId, toId);
    }
}
