<%@ page language="java" pageEncoding="utf-8" contentType="text/html; charset=utf-8" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="basePath" value="${pageContext.servletContext.contextPath }"/>

<div id="mysqlTaskRestart" class="modal">
</div>
<div class="page-content">
    <div class="row">
        <form id="add_form" class="form-horizontal" role="form">
            <div class="tabbable">
                <div class="tab-content" style="border: 0px">
                    <div id="basicId" class="tab-pane in active">

                        <div class="col-sm-12">
                            <div class="col-sm-4 form-group">
                                <label class="col-sm-4 control-label no-padding-right">任务类型</label>

                                <div class="col-sm-8">
                                    <select style="width:100%;">
                                        <option value="MYSQL" selected>MYSQL</option>
                                    </select>
                                </div>
                            </div>
                        </div>

                        <div class="col-sm-12">
                            <div class="col-sm-4 form-group">
                                <label class="col-sm-4 control-label no-padding-right"
                                       for="form-add-taskId">任务名称</label>

                                <div class="col-sm-8">
                                    <select multiple="" name="taskId" class="taskId tag-input-style"
                                            data-placeholder="Click to Choose..." id="form-add-taskId"
                                            style="width:100%;">
                                        <c:forEach items="${decorateList}" var="bean">
                                            <option value="${bean.id}">${bean.taskName} </option>
                                        </c:forEach>
                                    </select>
                                </div>
                            </div>

                            <div class="col-sm-4 form-group">
                                <label class="col-sm-4 control-label no-padding-right" for="form-add-sourceTableName">源表名称</label>

                                <div class="col-sm-8">
                                    <select multiple="" name="tableName" class="sourceTableName tag-input-style"
                                            data-placeholder="Click to Choose..." id="form-add-sourceTableName"
                                            style="width:100%;">
                                    </select>
                                </div>
                            </div>

                        </div>
                        <div class="col-sm-12">

                            <div class="col-sm-4 form-group">
                                <label class="col-sm-4 control-label no-padding-right" for="statement">表达式</label>

                                <div class="col-sm-8">
                                    <textarea name="statement" id="statement" class="col-xs-12" rows="5"/>
                                </div>
                            </div>

                            <div class="col-sm-4 form-group">
                                <label class="col-sm-4 control-label no-padding-right" for="remark">备注</label>

                                <div class="col-sm-8">
                                    <textarea name="remark" id="remark" class="col-xs-12" rows="5"/>
                                </div>
                            </div>
                        </div>

                        <div class="col-sm-12">
                            <p>源表名称不存在原因 1.任务名称没有对应的映射 2.存在映射但状态为无效</p>
                        </div>

                    </div>
                </div>
            </div>
        </form>
    </div>


    <div class="clearfix form-actions">
        <div class="col-md-offset-5 col-md-7">
            <button class="btn btn-info" type="button" onclick="doAdd();">
                <i class="ace-icon fa fa-check bigger-110"></i>
                保存
            </button>

            &nbsp; &nbsp; &nbsp;
            <button class="btn" type="reset" onclick="back2Main();">
                返回
                <i class="ace-icon fa fa-undo bigger-110"></i>
            </button>
        </div>
    </div>
</div>
<!-- /.page-content -->

<script type="text/javascript">
    var obj = null;
    var dualList = $('select[name="duallistbox_demo1[]"]').bootstrapDualListbox({
        infoTextFiltered: '<span class="label label-purple label-lg">Filtered</span>',
        infoText: false
    });
    var container = dualList.bootstrapDualListbox('getContainer');
    $('.taskId').css('min-width', '100%').select2({allowClear: false, maximumSelectionLength: 1, width: '100%'});
    $('.srcMediaSourceId').css('min-width', '100%').select2({
        allowClear: false,
        maximumSelectionLength: 1,
        width: '100%'
    });
    $('.sourceTableName').css('min-width', '100%').select2({
        allowClear: false,
        maximumSelectionLength: 1,
        width: '100%'
    });
    $("#mysqlTaskRestart").on('hide.bs.modal', function () {
        back2Main();
    });

    $('#form-add-taskId').change(function () {
        var taskId = $('#form-add-taskId').val();
        if (taskId == null) {
            $('#form-add-sourceTableName').html('');
            $("#copyTableName").html('');
            $('input[name=targetMediaNamespace]').val('');
            $(".srcMediaSourceId").val('').select2({allowClear: false, maximumSelectionLength: 1, width: '100%'});
            $(".targetMediaNamespaceId").val('').select2({allowClear: false, maximumSelectionLength: 1, width: '100%'});
            $(".sourceTableName").val('').select2({allowClear: false, maximumSelectionLength: 1, width: '100%'});
            return;
        }
        $.ajax({
            type: "post",
            url: "${basePath}/mediaMapping/getSourceDataBase",
            async: true,
            dataType: "json",
            data: "&taskId=" + taskId,
            success: function (result) {
                if (result != null && result != '') {
                    $('#form-add-srcMediaSourceId').html('');
                    $('#form-add-targetMediaNamespaceId').html('');
                    //$('#form-add-sourceTableName').html('');
                    $("#form-add-srcMediaSourceId").append("<option value=" + "'" + result.source.id + "'" + ">" + result.source.name + "</option>");
                    $(".srcMediaSourceId").val(result.source.id).select2({
                        allowClear: false,
                        maximumSelectionLength: 1,
                        width: '100%'
                    });

                    if (result.target != null && result.target.length > 0) {
                        for (var i = 0; i < result.target.length; i++) {
                            $("#form-add-targetMediaNamespaceId").append("<option value=" + "'" + result.target[i].id + "'" + ">" + result.target[i].name + "</option>");
                        }
                    }
                }
            }
        });

        $.ajax({
            type: "post",
            url: "${basePath}/decorate/findTables",
            async: true,
            dataType: "json",
            data: "&taskId=" + taskId,
            success: function (result) {
                if (result != null && result != '') {
                    $('#form-add-sourceTableName').html('');

                    for (var i = 0; i < result.length; i++) {
                        $("#form-add-sourceTableName").append("<option value=" + "'" + result[i] + "'" + ">" + result[i] + "</option>");
                    }
                }
            }
        });
    });


    function doAdd() {
        var taskId = $('#form-add-taskId').val();
        if (!validateForm()) {
            return;
        }
        $.ajax({
            type: "post",
            url: "${basePath}/decorate/doAddDecorate",
            dataType: "json",
            data: $("#add_form").serialize(),
            async: false,
            error: function (xhr, status, err) {
                alert(err);
            },
            success: function (data) {
                if (data == "success") {
                    alert("添加成功！");
                    back2Main();
                } else {
                    alert(data);
                }
            }
        });
    }


    function back2Main() {
        $("#add").hide();
        $("#mainContentInner").show();
        jobListTable.ajax.reload();
    }

    function validateForm() {
        if ($.trim($('#form-add-taskId').val()) == '') {
            alert('任务名称不能为空');
            return false;
        }

        if ($.trim($('#form-add-sourceTableName').val()) == '') {
            alert('表名称不可为空');
            return false;
        }

        if ($.trim($('#remark').val()) == '') {
            alert('备注不可为空');
            return false;
        }

        var statement = $.trim($('#statement').val());
        if (statement != null && statement != '' && !isSkipIds(statement)) {
            alert("请输入正确的主键ID格式，例如：1,3,5 或者 [1-5],[10-20]");
            return false;
        }

        return true;
    }

    function checkForSave() {
        var skipIds = $.trim($("#skipIdsHidden").val());
        if (skipIds != null && skipIds != '' && !isSkipIds(skipIds)) {
            alert("请输入正确的要跳过的主键ID格式，例如：1,3,5 或者 [1-5],[10-20]");
            return false;
        }
        return true;
    }

    function isSkipIds(val) {
        var skipIdsReg1 = /^([0-9]+,)*[0-9]+$/;
        var skipIdsReg2 = /^(\[[0-9]+\-[0-9]+],)*(\[[0-9]+\-[0-9]+])$/;
        return (skipIdsReg1.test(val) || skipIdsReg2.test(val));
    }


</script>
