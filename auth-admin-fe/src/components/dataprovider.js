import {stringify} from 'query-string';
import {fetchUtils} from "react-admin";

const apiUrl = "http://localhost:8081/management/v1";

const client = (url, options = {}) => {
    return fetchUtils.fetchJson(url, options);
};

const HTTP = {
    PUT: 'PUT',
    POST: 'POST',
    DELETE: 'DELETE',
    GET: 'GET',
    OPTIONS: 'OPTIONS'
}

export const SpringDataProvider = {
    getList: (resource, parameters) => {
        const {field, order} = parameters.sort || {};
        const {page, perPage} = parameters.pagination || {};

        const queryString = {
            'size': perPage,
            'page': page ? page - 1 : undefined,
            'sort': parameters.sort ? field + ',' + order : undefined,
            ...parameters.filter
        };

        const callUrl = `${apiUrl}/${resource}?${stringify(queryString)}`;

        return client(callUrl).then(({headers, json}) => ({
            data: json.content,
            total: json.totalElements
        }));
    },
    getOne: (resource, parameters) =>
        client(`${apiUrl}/${resource}/${parameters.id}`).then(({json}) => ({
            data: json,
        })),
    getMany: (resource, parameters) => {
        const queryString = {
            filter: parameters.ids ? JSON.stringify({id: parameters.ids}) : []
        };

        const callUrl = `${apiUrl}/${resource}?${stringify(queryString)}`;
        return client(callUrl).then(({json}) => ({data: json}));
    },
    getManyReference: (resource, parameters) => {
        const {page, perPage} = parameters.pagination;
        const query = {
            page: JSON.stringify(page),
            pageSize: JSON.stringify([(page - 1) * perPage, page * perPage - 1]),
        };
        const callUrl = `${apiUrl}/${resource}?${stringify(query)}`;

        return client(callUrl).then(({headers, json}) => ({
            data: json,
        }));
    },
    create: (resource, parameters) => {
        const callUrl = `${apiUrl}/${resource}`;
        return client(callUrl, {
            method: HTTP.POST,
            body: JSON.stringify(parameters.data),
        }).then(({json}) => ({
            data: {...parameters.data, id: json.id, response: json},
        }));
    },
    update: (resource, parameters) =>{
        const callUrl = `${apiUrl}/${resource}/${parameters.id}`
        return client(callUrl, {
            method: HTTP.PUT,
            body: JSON.stringify(parameters.data),
        }).then(({json}) => ({data: json}));
    },
    updateMany: (resource, parameters) => {
        const query = {
            filter: JSON.stringify({id: parameters.ids}),
        };
        const callUrl = `${apiUrl}/${resource}?${stringify(query)}`;
        return client(callUrl, {
            method: HTTP.PUT,
            body: JSON.stringify(parameters.data),
        }).then(({json}) => ({data: json}));
    },
    delete: (resource, parameters) => {
        const callUrl = `${apiUrl}/${resource}/${parameters.id}`;
        return client(callUrl, {
            method: HTTP.DELETE,
        }).then(({json}) => ({data: parameters.id}));
    },
    deleteMany: (resource, parameters) => {
        return Promise.all(
            parameters.ids.map(id => {
                const callUrl = `${apiUrl}/${resource}/${id}`;
                return client(callUrl, {
                        method: HTTP.DELETE,
                    });
                }
            )
        ).then(() => {
            return {data: parameters.ids};
        });
    },

};
