import * as React from "react";
import {
    TextInput,
    List,
    Datagrid,
    Edit,
    Create,
    SimpleForm,
    SingleFieldList,
    ChipField,
    BooleanInput,
    TextField,
    EditButton,
    ArrayInput,
    SimpleFormIterator
} from 'react-admin';
import {TextArrayField} from "../components/TextArrayField";

export const ClientList = (props) => (
    <List {...props}>
        <Datagrid>
            <TextField source="id"/>
            <TextField source="clientId"/>
            <TextArrayField source="redirectUris">
                <SingleFieldList>
                    <ChipField source="id"/>
                </SingleFieldList>
            </TextArrayField>
            <TextArrayField source="authorizationGrantTypes">
                <SingleFieldList>
                    <ChipField source="id"/>
                </SingleFieldList>
            </TextArrayField>
            <TextArrayField source="scopes">
                <SingleFieldList>
                    <ChipField source="id"/>
                </SingleFieldList>
            </TextArrayField>
            <TextField source="accessTokenLifetime"/>
            <TextField source="refreshTokenLifetime"/>
            <EditButton basePath="/clients"/>
        </Datagrid>
    </List>
);

const ClientTitle = ({record}) => {
    return <span>Client {record ? `${record.firstName} ${record.lastName}` : ''}</span>;
};

export const ClientEdit = (props) => (
    <Edit title={<ClientTitle/>} {...props}>
        <SimpleForm>
            <TextInput disabled source="id"/>
            <TextInput disabled source="clientId"/>
            <ArrayInput source="redirectUris">
                <SimpleFormIterator>
                    <TextInput source=""/>
                </SimpleFormIterator>
            </ArrayInput>
            <ArrayInput source="authorizationGrantTypes">
                <SimpleFormIterator>
                    <TextInput source=""/>
                </SimpleFormIterator>
            </ArrayInput>
            <ArrayInput source="scopes">
                <SimpleFormIterator>
                    <TextInput source=""/>
                </SimpleFormIterator>
            </ArrayInput>
            <TextInput source="accessTokenLifetime"/>
            <TextInput source="refreshTokenLifetime"/>
        </SimpleForm>
    </Edit>
);

export const ClientCreate = (props) => (
    <Create title="Create a Client" {...props}>
        <SimpleForm>
            <TextInput source="clientId"/>
            <TextInput source="secret"/>
            <ArrayInput source="redirectUris">
                <SimpleFormIterator>
                    <TextInput source=""/>
                </SimpleFormIterator>
            </ArrayInput>
            <ArrayInput source="authorizationGrantTypes">
                <SimpleFormIterator>
                    <TextInput source=""/>
                </SimpleFormIterator>
            </ArrayInput>
            <ArrayInput source="scopes">
                <SimpleFormIterator>
                    <TextInput source=""/>
                </SimpleFormIterator>
            </ArrayInput>
            <TextInput source="accessTokenLifetime"/>
            <TextInput source="refreshTokenLifetime"/>
        </SimpleForm>
    </Create>
);
