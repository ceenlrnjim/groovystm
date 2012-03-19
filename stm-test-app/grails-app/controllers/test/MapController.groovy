package test

class MapController {
    def statefulService

    def index = { }

    def get = {
        [value: statefulService.getValue(params.key)]
    }

    def add = {
        statefulService.addValue(params.key, params.value)
        [key: params.key, value: params.value]
    }
}
