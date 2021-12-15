import * as React from "react";
import { List, Datagrid, Edit, Create, SimpleForm, SelectInput, BooleanField, BooleanInput, TextField, EditButton, TextInput, DateInput } from 'react-admin';

export const ResourceList = (props) => (
    <List {...props}>
        <Datagrid>
            <TextField source="id" />
            <TextField source="name" />
            <TextField source="description" />
            <TextField source="host" />
            <TextField source="client_id" />
            <EditButton basePath="/resources" />
        </Datagrid>
    </List>
);

const ResourceTitle = ({ record }) => {
    return <span>User {record ? `${record.firstName} ${record.lastName}` : ''}</span>;
};

export const ResourceEdit = (props) => (
    <Edit title={<ResourceTitle />} {...props}>
        <SimpleForm>
            <TextInput disabled source="id" />
            <TextInput source="name" />
            <TextInput source="description" />
            <TextInput source="host" />
            <TextInput source="client_id" />
        </SimpleForm>
    </Edit>
);

export const ResourceCreate = (props) => (
    <Create title="Create a Resource" {...props}>
        <SimpleForm>
            <TextInput source="name" />
            <TextInput source="description" />
            <TextInput source="host" />
            <TextInput source="client_id" />
        </SimpleForm>
    </Create>
);
